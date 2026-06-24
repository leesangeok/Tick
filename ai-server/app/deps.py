"""Dependency wiring — port → adapter."""

from functools import lru_cache

from psycopg_pool import AsyncConnectionPool

from app.adapters.cache.noop_summary_cache import NoOpSummaryCacheAdapter
from app.adapters.cache.redis_summary_cache import RedisSummaryCacheAdapter
from app.adapters.embedding.openai_embedding_adapter import OpenAiEmbeddingAdapter
from app.adapters.llm.anthropic_claude_adapter import AnthropicClaudeAdapter
from app.adapters.observability.langfuse_trace_adapter import LangfuseTraceAdapter
from app.adapters.observability.noop_trace_adapter import NoOpTraceAdapter
from app.adapters.retriever.pgvector_retriever_adapter import PgvectorNewsRetrieverAdapter
from app.application.use_cases.embed_news import EmbedNewsUseCase
from app.application.use_cases.summarize_stock import SummarizeStockUseCase
from app.config.settings import settings
from app.ports.summary_cache_port import SummaryCachePort
from app.ports.trace_port import TracePort


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
def _trace() -> TracePort:
    # Langfuse 키가 설정돼 있으면 Langfuse, 없으면 NoOp.
    if settings.langfuse_public_key and settings.langfuse_secret_key:
        return LangfuseTraceAdapter()
    return NoOpTraceAdapter()


@lru_cache(maxsize=1)
def _cache() -> SummaryCachePort:
    # Redis host 가 설정돼 있으면 Redis, 없으면 NoOp (cache miss only).
    if settings.redis_host:
        return RedisSummaryCacheAdapter()
    return NoOpSummaryCacheAdapter()


def get_summarize_stock_use_case() -> SummarizeStockUseCase:
    return SummarizeStockUseCase(
        embedding=_embedding(),
        retriever=_retriever(),
        llm=_llm(),
        trace=_trace(),
        cache=_cache(),
    )


def get_embed_news_use_case() -> EmbedNewsUseCase:
    return EmbedNewsUseCase(
        embedding=_embedding(),
        retriever=_retriever(),
        trace=_trace(),
        cache=_cache(),
    )


async def open_pool() -> None:
    await _pool().open()


async def close_pool() -> None:
    await _pool().close()


def flush_langfuse() -> None:
    """Shutdown 시 큐에 남은 trace 를 flush. CLAUDE.md '스크립트에서 flush() 없으면 trace 안 감' 가드."""
    if settings.langfuse_public_key and settings.langfuse_secret_key:
        try:
            from langfuse import get_client

            get_client().flush()
        except Exception:
            pass
