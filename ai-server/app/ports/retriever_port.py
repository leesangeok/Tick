from dataclasses import dataclass
from typing import Protocol

from app.domain.models.retrieved_news import RetrievedNews
from app.domain.value_objects.stock_symbol import StockSymbol


@dataclass(frozen=True)
class RetrievalQuery:
    symbol: StockSymbol
    embedding: list[float]
    top_k: int
    days_window: int


class NewsRetrieverPort(Protocol):
    async def retrieve(self, query: RetrievalQuery) -> list[RetrievedNews]: ...
    async def upsert_missing_embeddings(
        self, symbol: StockSymbol, embed_fn, batch_limit: int = 50
    ) -> int: ...
