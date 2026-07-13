"""Postgres 기반 섹터 fallback — stock_master 조인으로 같은 sector 뉴스 최신순 조회.

backend 가 관리하는 stock_master 테이블을 read-only 로 참조. sector 컬럼은 이미 V1 migration
에서 정의됨.
"""

from datetime import datetime

from psycopg_pool import AsyncConnectionPool

from app.domain.models.retrieved_news import RetrievedNews
from app.domain.value_objects.stock_symbol import StockSymbol


class PgSectorFallbackAdapter:
    def __init__(self, pool: AsyncConnectionPool) -> None:
        self._pool = pool

    async def retrieve_sector_peers(
        self, symbol: StockSymbol, top_k: int, days_window: int
    ) -> list[RetrievedNews]:
        sql = """
            SELECT n.id, n.title, n.body, n.source, n.source_url, n.published_at
              FROM news n
              JOIN stock_master sm_peer ON sm_peer.symbol = n.symbol
             WHERE sm_peer.sector = (SELECT sector FROM stock_master WHERE symbol = %s)
               AND n.symbol != %s
               AND n.published_at >= NOW() - (%s || ' days')::interval
             ORDER BY n.published_at DESC
             LIMIT %s
        """
        async with self._pool.connection() as conn:
            async with conn.cursor() as cur:
                await cur.execute(
                    sql,
                    (symbol.value, symbol.value, days_window, top_k),
                )
                rows = await cur.fetchall()
        return [
            RetrievedNews(
                id=row[0],
                title=row[1],
                body=row[2],
                source=row[3],
                source_url=row[4],
                published_at=(
                    row[5] if isinstance(row[5], datetime) else datetime.fromisoformat(str(row[5]))
                ),
                # score 는 시간 기반 rank 표기 목적 (dense/sparse 개념 없음). recency 순 정렬됨.
                score=0.0,
            )
            for row in rows
        ]
