"""RAG orchestration: embed query → retrieve → build prompt → LLM."""

from dataclasses import dataclass

from app.embedding import embed
from app.llm import summarize
from app.retriever import RelevantNews, search


@dataclass
class Evidence:
    title: str
    source: str | None
    source_url: str | None
    published_at: str


@dataclass
class SummaryResult:
    summary: str
    evidences: list[Evidence]


def _build_query(symbol: str, stock_name: str) -> str:
    """retrieval 용 query — 종목명 + 변동/이유 키워드."""
    return f"{stock_name}({symbol}) 주가 변동 이유 실적 뉴스"


def _build_prompt(stock_name: str, symbol: str, news: list[RelevantNews]) -> str:
    if not news:
        return (
            f"{stock_name}({symbol}) 관련 최근 뉴스가 부족합니다. "
            f"근거 부족 시 '근거 부족으로 판단을 보류합니다' 라고만 응답하세요."
        )
    bullets = "\n".join(
        f"- [{n.published_at:%Y-%m-%d %H:%M}] {n.title}\n  {n.body}" for n in news
    )
    return (
        f"종목: {stock_name} ({symbol})\n\n"
        f"최근 관련 뉴스:\n{bullets}\n\n"
        f"위 뉴스들을 근거로, 이 종목의 최근 시장 흐름과 주목할 만한 사건을 "
        f"한국어로 2~4 문장 요약해 주세요."
    )


def generate(symbol: str, stock_name: str) -> SummaryResult:
    query = _build_query(symbol, stock_name)
    query_embedding = embed(query)
    relevant = search(symbol, query_embedding)
    prompt = _build_prompt(stock_name, symbol, relevant)
    summary = summarize(prompt)
    return SummaryResult(
        summary=summary,
        evidences=[
            Evidence(
                title=n.title,
                source=n.source,
                source_url=n.source_url,
                published_at=n.published_at.isoformat(),
            )
            for n in relevant
        ],
    )
