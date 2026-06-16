"""FastAPI 진입점. Spring backend 가 내부 docker network 로만 호출 (외부 노출 X)."""

import logging

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from app.config import settings
from app.retriever import upsert_missing_embeddings
from app.summary import generate

logging.basicConfig(level=settings.log_level)
log = logging.getLogger("tick.ai")

app = FastAPI(title="Tick AI", version="0.1.0")


class SummaryRequest(BaseModel):
    symbol: str
    stock_name: str


class EvidenceDto(BaseModel):
    title: str
    source: str | None
    source_url: str | None
    published_at: str


class SummaryResponse(BaseModel):
    summary: str
    evidences: list[EvidenceDto]


class EmbedResponse(BaseModel):
    upserted: int


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/ai/summary", response_model=SummaryResponse)
def post_summary(req: SummaryRequest) -> SummaryResponse:
    if not settings.anthropic_api_key or not settings.openai_api_key:
        raise HTTPException(status_code=503, detail="ai keys not configured")
    try:
        result = generate(req.symbol, req.stock_name)
    except Exception as e:
        log.exception("summary failed symbol=%s", req.symbol)
        raise HTTPException(status_code=502, detail=f"summary failed: {e}") from e
    return SummaryResponse(
        summary=result.summary,
        evidences=[
            EvidenceDto(
                title=e.title,
                source=e.source,
                source_url=e.source_url,
                published_at=e.published_at,
            )
            for e in result.evidences
        ],
    )


@app.post("/ai/embeddings/{symbol}", response_model=EmbedResponse)
def post_embeddings(symbol: str) -> EmbedResponse:
    """이 종목의 임베딩 안 된 뉴스에 대해 일괄 임베딩 생성 + 저장."""
    if not settings.openai_api_key:
        raise HTTPException(status_code=503, detail="openai key not configured")
    try:
        upserted = upsert_missing_embeddings(symbol)
    except Exception as e:
        log.exception("embed failed symbol=%s", symbol)
        raise HTTPException(status_code=502, detail=f"embed failed: {e}") from e
    return EmbedResponse(upserted=upserted)
