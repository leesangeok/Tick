from pydantic import BaseModel, Field


class StockSummaryRequest(BaseModel):
    symbol: str = Field(..., description="Stock code, e.g. 005930")
    stock_name: str = Field(..., description="Display name, e.g. 삼성전자")


class SummarySourceDto(BaseModel):
    title: str
    source: str | None
    source_url: str | None
    published_at: str


class StockSummaryResponse(BaseModel):
    symbol: str
    summary: str
    key_reasons: list[str] = []
    risk_notes: list[str] = []
    sources: list[SummarySourceDto] = []
    retrieved_count: int
