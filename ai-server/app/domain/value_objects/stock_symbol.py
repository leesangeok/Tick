from dataclasses import dataclass


@dataclass(frozen=True)
class StockSymbol:
    value: str

    def __post_init__(self) -> None:
        if not self.value or not self.value.strip():
            raise ValueError("StockSymbol cannot be blank")
