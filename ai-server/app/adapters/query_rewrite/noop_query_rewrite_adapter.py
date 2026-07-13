"""쿼리 재작성 비활성화 시 사용. base_query 그대로 반환."""

from app.domain.value_objects.stock_symbol import StockSymbol


class NoOpQueryRewriteAdapter:
    async def rewrite(
        self, symbol: StockSymbol, stock_name: str, base_query: str
    ) -> list[str]:
        return [base_query]
