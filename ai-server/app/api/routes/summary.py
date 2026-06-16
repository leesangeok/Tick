from fastapi import APIRouter, Depends, HTTPException

from app.api.schemas.summary import (
    StockSummaryRequest,
    StockSummaryResponse,
    SummarySourceDto,
)
from app.application.commands.summarize_stock_command import SummarizeStockCommand
from app.application.use_cases.summarize_stock import SummarizeStockUseCase
from app.config.settings import settings
from app.deps import get_summarize_stock_use_case
from app.domain.value_objects.stock_symbol import StockSymbol

router = APIRouter()


@router.post("/ai/summary", response_model=StockSummaryResponse)
async def post_summary(
    req: StockSummaryRequest,
    use_case: SummarizeStockUseCase = Depends(get_summarize_stock_use_case),
) -> StockSummaryResponse:
    if not settings.anthropic_api_key or not settings.openai_api_key:
        raise HTTPException(status_code=503, detail="ai keys not configured")

    try:
        symbol = StockSymbol(req.symbol)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e)) from e

    result = await use_case.execute(
        SummarizeStockCommand(symbol=symbol, stock_name=req.stock_name)
    )
    s = result.summary
    return StockSummaryResponse(
        symbol=s.symbol,
        summary=s.summary,
        key_reasons=s.key_reasons,
        risk_notes=s.risk_notes,
        sources=[
            SummarySourceDto(
                title=src.title,
                source=src.source,
                source_url=src.source_url,
                published_at=src.published_at,
            )
            for src in s.sources
        ],
        retrieved_count=result.retrieved_count,
    )
