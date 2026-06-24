"""Redis 미설정 환경에서 사용. cache miss 만 반환."""

import logging

from app.application.results.stock_summary_result import StockSummaryResult
from app.domain.value_objects.stock_symbol import StockSymbol

log = logging.getLogger("tick.ai.cache")


class NoOpSummaryCacheAdapter:
    async def get(self, symbol: StockSymbol) -> StockSummaryResult | None:
        return None

    async def set(self, symbol: StockSymbol, result: StockSummaryResult) -> None:
        log.debug("noop cache set symbol=%s", symbol.value)

    async def invalidate(self, symbol: StockSymbol) -> None:
        log.debug("noop cache invalidate symbol=%s", symbol.value)
