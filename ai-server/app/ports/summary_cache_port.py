from typing import Protocol

from app.application.results.stock_summary_result import StockSummaryResult
from app.domain.value_objects.stock_symbol import StockSymbol


class SummaryCachePort(Protocol):
    """종목 AI 요약 응답 캐시.

    Anthropic prompt cache 는 input token 일부만 할인. 호출 자체를 줄이려면 응답
    전체를 우리가 캐싱해야 한다. backend 와 같은 Redis 인스턴스 공유, key prefix
    로 namespace 분리.
    """

    async def get(self, symbol: StockSymbol) -> StockSummaryResult | None: ...

    async def set(self, symbol: StockSymbol, result: StockSummaryResult) -> None: ...

    async def invalidate(self, symbol: StockSymbol) -> None: ...
