from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class RetrievedNews:
    """Retriever 가 반환하는 한 건의 근거 뉴스."""

    id: int
    title: str
    body: str
    source: str | None
    source_url: str | None
    published_at: datetime
    score: float  # cosine distance; 0 에 가까울수록 유사
