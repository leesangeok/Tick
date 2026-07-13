from pydantic import BaseModel, Field


class StockSummaryRequest(BaseModel):
    symbol: str = Field(..., description="Stock code, e.g. 005930")
    stock_name: str = Field(..., description="Display name, e.g. 삼성전자")


class SummarySourceDto(BaseModel):
    news_id: int = Field(..., description="DB news.id — 원문 조회/딥링크 용")
    title: str
    source: str | None
    source_url: str | None
    published_at: str


class KeyReasonDto(BaseModel):
    text: str = Field(..., description="근거 문장 (원문 그대로, `[뉴스 N번]` 마커 포함)")
    source_indices: list[int] = Field(
        default_factory=list,
        description=(
            "sources 배열의 1-based 인덱스. 프론트가 `sources[i-1]` 로 lookup 하여 하이라이트."
        ),
    )


class StockSummaryResponse(BaseModel):
    symbol: str
    summary: str
    key_reasons: list[KeyReasonDto] = []
    risk_notes: list[str] = []
    sources: list[SummarySourceDto] = []
    retrieved_count: int
