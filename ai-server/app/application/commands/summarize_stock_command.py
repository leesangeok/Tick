from dataclasses import dataclass

from app.domain.value_objects.stock_symbol import StockSymbol


@dataclass(frozen=True)
class SummarizeStockCommand:
    symbol: StockSymbol
    stock_name: str
