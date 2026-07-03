from typing import Protocol

from app.domain.models.retrieved_news import RetrievedNews


class RerankerPort(Protocol):
    """Retriever 가 뽑은 후보 문서를 query 관련도로 재정렬해 top_n 개를 반환."""

    async def rerank(
        self, query: str, candidates: list[RetrievedNews], top_n: int
    ) -> list[RetrievedNews]: ...
