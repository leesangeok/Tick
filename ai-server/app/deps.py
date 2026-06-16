"""싱글턴 클라이언트 — 매 요청마다 새로 만들지 않음."""

from functools import lru_cache

from anthropic import Anthropic
from openai import OpenAI
from psycopg_pool import ConnectionPool

from app.config import settings


@lru_cache(maxsize=1)
def openai_client() -> OpenAI:
    return OpenAI(api_key=settings.openai_api_key)


@lru_cache(maxsize=1)
def anthropic_client() -> Anthropic:
    return Anthropic(api_key=settings.anthropic_api_key)


@lru_cache(maxsize=1)
def pg_pool() -> ConnectionPool:
    # min 1 / max 10 — 단일 노드 + 가벼운 트래픽 가정
    return ConnectionPool(conninfo=settings.postgres_dsn, min_size=1, max_size=10, open=True)
