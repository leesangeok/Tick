"""Golden set 을 순회하며 summarize_stock 을 실행하고 LLM judge 로 채점.

Usage:
    cd ai-server
    uv run python -m evals.run_eval --label baseline-naver-only
    uv run python -m evals.run_eval --label after-dart --top-k 8

결과: evals/results/YYYY-MM-DD_<label>.json

env: OPENAI_API_KEY, ANTHROPIC_API_KEY, POSTGRES_DSN 필수.
백엔드가 뉴스를 이미 embed 해둔 상태여야 한다.
"""

import argparse
import asyncio
import json
import statistics
import time
from dataclasses import asdict, is_dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from app.config.settings import settings

# deps.py 는 lru_cache 로 embedding/retriever/llm 을 노출. eval 은 내부 도구라 private 참조 허용.
from app.deps import (
    _embedding,
    _llm,
    _retriever,
    close_pool,
    flush_langfuse,
    open_pool,
)
from app.domain.value_objects.stock_symbol import StockSymbol
from app.ports.retriever_port import RetrievalQuery
from evals import schemas
from evals.judge import JUDGE_MODEL, judge_summary

EVAL_DIR = Path(__file__).parent
GOLDEN_SET = EVAL_DIR / "golden_set.jsonl"
RESULTS_DIR = EVAL_DIR / "results"


def load_golden(path: Path) -> list[schemas.GoldenItem]:
    items: list[schemas.GoldenItem] = []
    with path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            items.append(schemas.GoldenItem(**json.loads(line)))
    return items


async def evaluate_one(
    item: schemas.GoldenItem, top_k: int, days_window: int
) -> schemas.EvalRecord:
    started = time.perf_counter()
    try:
        symbol = StockSymbol(item.symbol)
    except ValueError as e:
        return schemas.EvalRecord(
            symbol=item.symbol,
            stock_name=item.stock_name,
            tier=item.tier,
            ok=False,
            error=str(e),
            summary=None,
            key_reasons=[],
            risk_notes=[],
            source_count=0,
            source_breakdown={},
            retrieval_top_k=top_k,
            elapsed_ms=0,
            judge=None,
        )

    embedding = _embedding()
    retriever = _retriever()
    llm = _llm()

    query_text = f"{item.stock_name}({item.symbol}) 주가 변동 이유 실적 뉴스"
    qvec = await embedding.embed(query_text)
    news = await retriever.retrieve(
        RetrievalQuery(
            symbol=symbol,
            embedding=qvec,
            top_k=top_k,
            days_window=days_window,
            raw_query_text=query_text,
        )
    )

    breakdown: dict[str, int] = {}
    for n in news:
        key = (n.source or "unknown").upper()
        breakdown[key] = breakdown.get(key, 0) + 1

    if not news:
        return schemas.EvalRecord(
            symbol=item.symbol,
            stock_name=item.stock_name,
            tier=item.tier,
            ok=True,
            error=None,
            summary=None,
            key_reasons=[],
            risk_notes=[],
            source_count=0,
            source_breakdown=breakdown,
            retrieval_top_k=top_k,
            elapsed_ms=int((time.perf_counter() - started) * 1000),
            judge=None,
        )

    ai = await llm.generate_stock_summary(symbol=symbol, stock_name=item.stock_name, news=news)
    verdict = await judge_summary(
        symbol=item.symbol,
        stock_name=item.stock_name,
        summary=ai.summary,
        key_reasons=ai.key_reasons,
        risk_notes=ai.risk_notes,
        news=news,
        expected_topics=item.expected_topics,
        known_traps=item.known_hallucination_traps,
    )
    return schemas.EvalRecord(
        symbol=item.symbol,
        stock_name=item.stock_name,
        tier=item.tier,
        ok=True,
        error=None,
        summary=ai.summary,
        key_reasons=ai.key_reasons,
        risk_notes=ai.risk_notes,
        source_count=len(news),
        source_breakdown=breakdown,
        retrieval_top_k=top_k,
        elapsed_ms=int((time.perf_counter() - started) * 1000),
        judge=verdict,
    )


def aggregate(records: list[schemas.EvalRecord]) -> dict[str, Any]:
    judged = [r for r in records if r.judge is not None]
    base = {
        "count_total": len(records),
        "count_judged": len(judged),
        "count_no_news": sum(1 for r in records if r.ok and r.source_count == 0),
        "count_error": sum(1 for r in records if not r.ok),
    }
    if not judged:
        return base

    def mean(pick):
        return round(statistics.mean(pick(r) for r in judged), 3)

    base.update(
        {
            "groundedness_mean": mean(lambda r: r.judge.groundedness),
            "citation_accuracy_mean": mean(lambda r: r.judge.citation_accuracy),
            "hallucination_count_sum": sum(r.judge.hallucination_count for r in judged),
            "hallucination_count_mean": mean(lambda r: r.judge.hallucination_count),
            "coverage_mean": mean(lambda r: r.judge.coverage),
            # 판정 안정성 관측 — 재판정 트리거된 종목 수와 평균 판정 횟수.
            "count_retried": sum(1 for r in judged if r.judge.retry_triggered),
            "avg_judge_runs": round(
                statistics.mean(r.judge.judge_runs for r in judged), 2
            ),
            "groundedness_std_mean": round(
                statistics.mean(r.judge.groundedness_std for r in judged), 3
            ),
        }
    )

    # tier 별 세부 집계
    tiers: dict[str, list[schemas.EvalRecord]] = {}
    for r in judged:
        tiers.setdefault(r.tier, []).append(r)
    base["by_tier"] = {
        t: {
            "count": len(rs),
            "groundedness_mean": round(statistics.mean(r.judge.groundedness for r in rs), 3),
            "hallucination_count_sum": sum(r.judge.hallucination_count for r in rs),
        }
        for t, rs in tiers.items()
    }
    return base


def _to_jsonable(obj: Any) -> Any:
    if is_dataclass(obj):
        return asdict(obj)
    return obj


async def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--label", required=True, help="결과 파일 라벨. 예: baseline-naver-only")
    parser.add_argument("--top-k", type=int, default=settings.retrieval_top_k)
    parser.add_argument("--days-window", type=int, default=settings.retrieval_days_window)
    parser.add_argument("--golden", type=Path, default=GOLDEN_SET)
    args = parser.parse_args()

    if not settings.anthropic_api_key or not settings.openai_api_key:
        raise SystemExit("ANTHROPIC_API_KEY / OPENAI_API_KEY 환경변수 필요")

    items = load_golden(args.golden)
    print(
        f"[eval] {len(items)} items | label={args.label} | top_k={args.top_k} | "
        f"days_window={args.days_window} | llm={settings.anthropic_model} | "
        f"judge={JUDGE_MODEL} x{settings.judge_repeat}"
    )

    await open_pool()
    started_at = datetime.now(UTC).isoformat()
    records: list[schemas.EvalRecord] = []
    try:
        for i, item in enumerate(items, 1):
            print(
                f"  [{i}/{len(items)}] {item.symbol} {item.stock_name} ({item.tier})...",
                flush=True,
            )
            rec = await evaluate_one(item, args.top_k, args.days_window)
            records.append(rec)
            if rec.judge is not None:
                j = rec.judge
                retry_tag = " [RETRY]" if j.retry_triggered else ""
                print(
                    f"    src={rec.source_count} breakdown={rec.source_breakdown} "
                    f"grounded={j.groundedness}±{j.groundedness_std} "
                    f"cite={j.citation_accuracy} halluc={j.hallucination_count} "
                    f"cov={j.coverage} runs={j.judge_runs}{retry_tag}"
                )
            elif rec.ok:
                print("    (뉴스 0건 — judge 스킵)")
            else:
                print(f"    ERROR: {rec.error}")
    finally:
        await close_pool()
        flush_langfuse()

    finished_at = datetime.now(UTC).isoformat()
    run = schemas.EvalRun(
        label=args.label,
        started_at=started_at,
        finished_at=finished_at,
        top_k=args.top_k,
        days_window=args.days_window,
        llm_model=settings.anthropic_model,
        judge_model=JUDGE_MODEL,
        aggregate=aggregate(records),
        records=records,
    )

    RESULTS_DIR.mkdir(exist_ok=True)
    date_str = datetime.now(UTC).strftime("%Y-%m-%d")
    out = RESULTS_DIR / f"{date_str}_{args.label}.json"
    out.write_text(
        json.dumps(_to_jsonable(run), ensure_ascii=False, indent=2, default=_to_jsonable),
        encoding="utf-8",
    )
    print(f"\n[eval] saved: {out}")
    print(f"[eval] aggregate: {json.dumps(run.aggregate, ensure_ascii=False)}")


if __name__ == "__main__":
    asyncio.run(main())
