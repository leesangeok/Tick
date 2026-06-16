"""Dependency wiring — port → adapter."""

from functools import lru_cache

from psycopg_pool import AsyncConnectionPool

from app.adapters.embedding.openai_embedding_adapter import OpenAiEmbeddingAdapter
from app.adapters.llm.anthropic_claude_adapter import AnthropicClaudeAdapter
from app.adapters.observability.noop_trace_adapter import NoOpTraceAdapter
from app.adapters.retriever.pgvector_retriever_adapter import PgvectorNewsRetrieverAdapter
from app.application.use_cases.embed_news import EmbedNewsUseCase
from app.application.use_cases.summarize_stock import SummarizeStockUseCase
from app.config.settings import settings


@lru_cache(maxsize=1)
def _pool() -> AsyncConnectionPool:
    # open=False → 첫 요청에 lazy open. uvicorn worker fork 안전.
    pool = AsyncConnectionPool(conninfo=settings.postgres_dsn, min_size=1, max_size=10, open=False)
    return pool


@lru_cache(maxsize=1)
def _embedding() -> OpenAiEmbeddingAdapter:
    return OpenAiEmbeddingAdapter()


@lru_cache(maxsize=1)
def _llm() -> AnthropicClaudeAdapter:
    return AnthropicClaudeAdapter()


@lru_cache(maxsize=1)
def _retriever() -> PgvectorNewsRetrieverAdapter:
    return PgvectorNewsRetrieverAdapter(pool=_pool())


@lru_cache(maxsize=1)
def _trace() -> NoOpTraceAdapter:
    return NoOpTraceAdapter()


def get_summarize_stock_use_case() -> SummarizeStockUseCase:
    return SummarizeStockUseCase(
        embedding=_embedding(),
        retriever=_retriever(),
        llm=_llm(),
        trace=_trace(),
    )


def get_embed_news_use_case() -> EmbedNewsUseCase:
    return EmbedNewsUseCase(
        embedding=_embedding(),
        retriever=_retriever(),
        trace=_trace(),
    )


async def open_pool() -> None:
    await _pool().open()


async def close_pool() -> None:
    await _pool().close()
