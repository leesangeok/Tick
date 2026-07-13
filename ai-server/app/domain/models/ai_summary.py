from dataclasses import dataclass, field

from app.domain.models.retrieved_news import RetrievedNews


@dataclass(frozen=True)
class SummarySource:
    news_id: int  # DB news.id — 프론트가 원문 링크/하이라이트에 사용.
    title: str
    source: str | None
    source_url: str | None
    published_at: str  # ISO8601


@dataclass(frozen=True)
class KeyReason:
    """근거 문장 + 뒷받침 뉴스 인덱스.

    source_indices 는 sources 배열의 1-based 인덱스 (프롬프트 `[뉴스 N번]` 마커와 일치).
    프론트는 `sources[i-1]` 로 실제 뉴스 정보 (news_id, url, 제목) 를 조회하고 하이라이트.
    """

    text: str
    source_indices: list[int] = field(default_factory=list)


@dataclass(frozen=True)
class AiSummary:
    symbol: str
    summary: str
    key_reasons: list[KeyReason] = field(default_factory=list)
    risk_notes: list[str] = field(default_factory=list)
    sources: list[SummarySource] = field(default_factory=list)

    @staticmethod
    def sources_from(news: list[RetrievedNews]) -> list[SummarySource]:
        return [
            SummarySource(
                news_id=n.id,
                title=n.title,
                source=n.source,
                source_url=n.source_url,
                published_at=n.published_at.isoformat(),
            )
            for n in news
        ]
