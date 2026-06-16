from dataclasses import dataclass

from app.domain.models.ai_summary import AiSummary


@dataclass(frozen=True)
class StockSummaryResult:
    summary: AiSummary
    retrieved_count: int
