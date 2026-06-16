from dataclasses import dataclass


@dataclass(frozen=True)
class EmbedResult:
    upserted: int
