from dataclasses import dataclass

from app.domain.value_objects.stock_symbol import StockSymbol


@dataclass(frozen=True)
class EmbedNewsCommand:
    symbol: StockSymbol
    batch_limit: int = 50
