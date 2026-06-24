"""Redis 기반 종목 AI 요약 캐시.

- key: `ai:summary:{symbol}` — backend 의 `stock:quote:*` / `auth:refresh:*` 와 namespace 분리.
- value: dataclass → dict → JSON 으로 직렬화. 다음 호출 시 dict → dataclass 복원.
- TTL: settings.summary_cache_ttl_sec (기본 15분). `embed_news` 후 자동 invalidate.
- Redis 다운 시 모든 메서드 graceful (None 반환 / 조용히 skip). 도메인 흐름 안 끊김.
"""

import json
import logging
from dataclasses import asdict

import redis.asyncio as redis_async
from redis.exceptions import RedisError

from app.application.results.stock_summary_result import StockSummaryResult
from app.config.settings import settings
from app.domain.models.ai_summary import AiSummary, SummarySource
from app.domain.value_objects.stock_symbol import StockSymbol

log = logging.getLogger("tick.ai.cache")

KEY_PREFIX = "ai:summary:"


class RedisSummaryCacheAdapter:
    def __init__(self) -> None:
        self._client = redis_async.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password or None,
            decode_responses=True,
            socket_connect_timeout=2,
            socket_timeout=2,
        )
        self._ttl_sec = settings.summary_cache_ttl_sec

    async def get(self, symbol: StockSymbol) -> StockSummaryResult | None:
        try:
            raw = await self._client.get(_key(symbol))
        except RedisError as e:
            log.warning("redis get failed symbol=%s err=%s", symbol.value, e)
            return None
        if raw is None:
            return None
        try:
            payload = json.loads(raw)
            return _to_result(payload)
        except (json.JSONDecodeError, KeyError, TypeError) as e:
            # 캐시 schema 변경 직후 등으로 깨진 값. 그냥 miss 처리.
            log.warning("cache payload broken symbol=%s err=%s", symbol.value, e)
            return None

    async def set(self, symbol: StockSymbol, result: StockSummaryResult) -> None:
        try:
            await self._client.set(_key(symbol), json.dumps(_to_dict(result)), ex=self._ttl_sec)
        except RedisError as e:
            log.warning("redis set failed symbol=%s err=%s", symbol.value, e)

    async def invalidate(self, symbol: StockSymbol) -> None:
        try:
            await self._client.delete(_key(symbol))
        except RedisError as e:
            log.warning("redis delete failed symbol=%s err=%s", symbol.value, e)


def _key(symbol: StockSymbol) -> str:
    return f"{KEY_PREFIX}{symbol.value}"


def _to_dict(result: StockSummaryResult) -> dict:
    return {
        "retrieved_count": result.retrieved_count,
        "summary": asdict(result.summary),
    }


def _to_result(payload: dict) -> StockSummaryResult:
    s = payload["summary"]
    summary = AiSummary(
        symbol=s["symbol"],
        summary=s["summary"],
        key_reasons=list(s.get("key_reasons", [])),
        risk_notes=list(s.get("risk_notes", [])),
        sources=[SummarySource(**src) for src in s.get("sources", [])],
    )
    return StockSummaryResult(summary=summary, retrieved_count=int(payload["retrieved_count"]))
