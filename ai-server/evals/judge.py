"""LLM-as-a-judge — Claude Sonnet 이 요약을 뉴스 근거 기준으로 채점.

Summary 생성 모델(Haiku)과 다른 (더 큰) 모델을 쓰는 게 정석. Sonnet 4.6 사용.

Stochasticity 완화: 같은 입력에도 판정이 흔들리는 걸 (특히 삼성바이오처럼 `grounded=0.0`
outlier) 완화하기 위해 N회 병렬 호출 후 지표별 median 을 반환한다 (n=settings.judge_repeat).
Hallucination_examples 는 union, notes 는 median-of-N 명시.
"""

import asyncio
import json
import statistics
from typing import Any

from anthropic import AsyncAnthropic

from app.config.settings import settings
from app.domain.models.retrieved_news import RetrievedNews
from evals.schemas import JudgeVerdict

JUDGE_MODEL = "claude-sonnet-4-6"
JUDGE_MAX_TOKENS = 1000

JUDGE_SYSTEM = """당신은 LLM 요약 결과의 품질을 채점하는 감사원입니다.

주어진 검색 뉴스만을 진실의 근거로 사용합니다. 뉴스 밖의 상식이나 최신 정보는 참고하지 마세요.

채점 기준 (모두 정확한 숫자로 반환):
- groundedness (0.0~1.0): 요약 문장의 각 주장이 뉴스 안에 있는가.
- citation_accuracy (0.0~1.0): key_reasons 각 항목이 뉴스로 직접 뒷받침되는가.
- hallucination_count (int, 0 이상): 뉴스에 없는 사실 주장 개수.
  뉴스에 없는 숫자/이름/M&A/실적/일정 등.
- hallucination_examples: 해당 사례를 원문 그대로 짧게 인용.
- coverage (0.0~1.0): 뉴스에 담긴 핵심 내용이 요약에 반영된 정도.
  근거는 있는데 요약이 얕으면 낮게.
- notes: 20자 이내 요약 코멘트.

투자 권유 표현("매수하세요", "확실히 오릅니다", "수익 보장")이 있으면
groundedness 상관없이 hallucination_count 에 1 이상 더하고 notes 에 명시.

출력은 JSON 하나만. 코드펜스 금지.
{
  "groundedness": 0.0,
  "citation_accuracy": 0.0,
  "hallucination_count": 0,
  "hallucination_examples": ["..."],
  "coverage": 0.0,
  "notes": "..."
}"""


async def judge_summary(
    symbol: str,
    stock_name: str,
    summary: str,
    key_reasons: list[str],
    risk_notes: list[str],
    news: list[RetrievedNews],
    expected_topics: list[str] | None = None,
    known_traps: list[str] | None = None,
    repeats: int | None = None,
) -> JudgeVerdict:
    """N회 병렬 판정 후 지표별 median. 판정 편차 (σ) 가 크면 자동으로 extras 회 추가 판정.

    Sonnet 4.6 이 같은 입력에도 개별 판정에 상당한 stochasticity 를 보이는 케이스가
    있어 3회 median 만으로는 완전 해결되지 않는다 (NAVER 등). σ 큰 종목만 선택적으로
    재판정하는 게 전체 비용은 유지하면서 안정성을 확보하는 실무적 접근.
    """
    n = repeats if repeats is not None else settings.judge_repeat
    args = (
        symbol, stock_name, summary, key_reasons, risk_notes,
        news, expected_topics, known_traps,
    )
    if n <= 1:
        return await _judge_once(*args)

    verdicts = await asyncio.gather(*[_judge_once(*args) for _ in range(n)])
    initial = _aggregate(verdicts)

    # 판정 편차 큰 경우 자동 재판정.
    extras = settings.judge_retry_extras
    if extras > 0 and _should_retry(initial):
        more = await asyncio.gather(*[_judge_once(*args) for _ in range(extras)])
        verdicts.extend(more)
        retried = _aggregate(verdicts)
        retried.retry_triggered = True
        return retried
    return initial


def _should_retry(v: JudgeVerdict) -> bool:
    gt = settings.judge_std_threshold_grounded
    ht = settings.judge_std_threshold_halluc
    return v.groundedness_std >= gt or v.hallucination_count_std >= ht


async def _judge_once(
    symbol: str,
    stock_name: str,
    summary: str,
    key_reasons: list[str],
    risk_notes: list[str],
    news: list[RetrievedNews],
    expected_topics: list[str] | None,
    known_traps: list[str] | None,
) -> JudgeVerdict:
    client = AsyncAnthropic(api_key=settings.anthropic_api_key)
    prompt = _build_prompt(
        symbol, stock_name, summary, key_reasons, risk_notes, news, expected_topics, known_traps
    )
    resp = await client.messages.create(
        model=JUDGE_MODEL,
        max_tokens=JUDGE_MAX_TOKENS,
        system=JUDGE_SYSTEM,
        messages=[{"role": "user", "content": prompt}],
    )
    text = "".join(b.text for b in resp.content if getattr(b, "type", None) == "text")
    parsed = _parse_json(text)
    return JudgeVerdict(
        groundedness=_clamp01(parsed.get("groundedness", 0.0)),
        citation_accuracy=_clamp01(parsed.get("citation_accuracy", 0.0)),
        hallucination_count=max(0, int(parsed.get("hallucination_count", 0))),
        hallucination_examples=[str(x) for x in parsed.get("hallucination_examples", [])],
        coverage=_clamp01(parsed.get("coverage", 0.0)),
        notes=str(parsed.get("notes", "")),
    )


def _aggregate(verdicts: list[JudgeVerdict]) -> JudgeVerdict:
    """N 판정의 지표별 median + 편차.

    examples 는 dedup union, notes 는 median 태그 + 첫 판정 notes.
    """
    n = len(verdicts)
    grounded = [v.groundedness for v in verdicts]
    cite = [v.citation_accuracy for v in verdicts]
    halluc = [v.hallucination_count for v in verdicts]
    cov = [v.coverage for v in verdicts]

    seen: set[str] = set()
    examples: list[str] = []
    for v in verdicts:
        for e in v.hallucination_examples:
            if e not in seen:
                seen.add(e)
                examples.append(e)

    head_notes = verdicts[0].notes if verdicts and verdicts[0].notes else ""
    g_std = round(statistics.pstdev(grounded), 3) if n > 1 else 0.0
    h_std = round(statistics.pstdev(halluc), 3) if n > 1 else 0.0
    return JudgeVerdict(
        groundedness=round(statistics.median(grounded), 3),
        citation_accuracy=round(statistics.median(cite), 3),
        hallucination_count=int(statistics.median(halluc)),
        hallucination_examples=examples,
        coverage=round(statistics.median(cov), 3),
        notes=f"median of {n} judges | {head_notes}".strip(" |"),
        judge_runs=n,
        groundedness_std=g_std,
        hallucination_count_std=h_std,
        retry_triggered=False,
    )


def _build_prompt(
    symbol: str,
    stock_name: str,
    summary: str,
    key_reasons: list[str],
    risk_notes: list[str],
    news: list[RetrievedNews],
    expected_topics: list[str] | None,
    known_traps: list[str] | None,
) -> str:
    news_block = "\n\n".join(
        f"[{i + 1}] [{n.source or 'N/A'}] [{n.published_at:%Y-%m-%d %H:%M}] "
        f"{n.title}\n{n.body[:500]}"
        for i, n in enumerate(news)
    ) or "(뉴스 없음)"
    hints = ""
    if expected_topics:
        hints += f"\n관련 기대 토픽 (참고용, 정답 아님): {', '.join(expected_topics)}"
    if known_traps:
        hints += f"\nhallucination 함정 (참고): {', '.join(known_traps)}"
    return f"""종목: {stock_name} ({symbol}){hints}

검색된 뉴스 (진실의 근거):
{news_block}

생성된 요약:
summary: {summary}
key_reasons: {json.dumps(key_reasons, ensure_ascii=False)}
risk_notes: {json.dumps(risk_notes, ensure_ascii=False)}

위 뉴스만을 근거로 요약을 채점하세요. JSON 만 출력."""


def _clamp01(x: Any) -> float:
    try:
        v = float(x)
    except (TypeError, ValueError):
        return 0.0
    return max(0.0, min(1.0, v))


def _parse_json(text: str) -> dict[str, Any]:
    s = text.strip()
    if s.startswith("```"):
        s = s.split("```", 2)[1]
        if s.startswith("json"):
            s = s[4:]
        s = s.rsplit("```", 1)[0]
    s = s.strip()
    try:
        return json.loads(s)
    except json.JSONDecodeError:
        i, j = s.find("{"), s.rfind("}")
        if i != -1 and j > i:
            try:
                return json.loads(s[i : j + 1])
            except json.JSONDecodeError:
                return {}
        return {}
