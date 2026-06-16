from dataclasses import dataclass, field

from app.domain.models.retrieved_news import RetrievedNews


@dataclass(frozen=True)
class SummarySource:
    title: str
    source: str | None
    source_url: str | None
    published_at: str  # ISO8601


@dataclass(frozen=True)
class AiSummary:
    symbol: str
    summary: str
    key_reasons: list[str] = field(default_factory=list)
    risk_notes: list[str] = field(default_factory=list)
    sources: list[SummarySource] = field(default_factory=list)

    @staticmethod
    def sources_from(news: list[RetrievedNews]) -> list[SummarySource]:
        return [
            SummarySource(
                title=n.title,
                source=n.source,
                source_url=n.source_url,
                published_at=n.published_at.isoformat(),
            )
            for n in news
        ]
