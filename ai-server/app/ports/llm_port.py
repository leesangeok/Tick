from typing import Protocol

from app.domain.models.ai_summary import AiSummary
from app.domain.models.retrieved_news import RetrievedNews
from app.domain.value_objects.stock_symbol import StockSymbol


class LlmPort(Protocol):
    async def generate_stock_summary(
        self, symbol: StockSymbol, stock_name: str, news: list[RetrievedNews]
    ) -> AiSummary: ...
