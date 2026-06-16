from fastapi import APIRouter, Depends, HTTPException

from app.api.schemas.embed import EmbedResponse
from app.application.commands.embed_news_command import EmbedNewsCommand
from app.application.use_cases.embed_news import EmbedNewsUseCase
from app.config.settings import settings
from app.deps import get_embed_news_use_case
from app.domain.value_objects.stock_symbol import StockSymbol

router = APIRouter()


@router.post("/ai/embeddings/{symbol}", response_model=EmbedResponse)
async def post_embeddings(
    symbol: str,
    use_case: EmbedNewsUseCase = Depends(get_embed_news_use_case),
) -> EmbedResponse:
    if not settings.openai_api_key:
        raise HTTPException(status_code=503, detail="openai key not configured")
    result = await use_case.execute(EmbedNewsCommand(symbol=StockSymbol(symbol)))
    return EmbedResponse(upserted=result.upserted)
