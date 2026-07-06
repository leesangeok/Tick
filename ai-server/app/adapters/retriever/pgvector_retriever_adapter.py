"""pgvector retriever (직접 psycopg async).

LangChain langchain-postgres.PGVector 도 후보였으나 sqlalchemy 의존성 + 우리 backend 와 같은
news / news_embedding 스키마를 그대로 쓰는 게 명시적이라 직접 구현. CLAUDE.md 의
\"retriever wrapper 보조 허용\" 범위 안.
"""

from collections.abc import Awaitable, Callable
from datetime import datetime

from pgvector.psycopg import register_vector_async
from psycopg_pool import AsyncConnectionPool

from app.config.settings import settings
from app.domain.models.retrieved_news import RetrievedNews
from app.domain.value_objects.stock_symbol import StockSymbol
from app.ports.retriever_port import RetrievalQuery


class PgvectorNewsRetrieverAdapter:
    def __init__(self, pool: AsyncConnectionPool) -> None:
        self._pool = pool

    async def retrieve(self, query: RetrievalQuery) -> list[RetrievedNews]:
        sql = """
            SELECT n.id, n.title, n.body, n.source, n.source_url, n.published_at,
                   ne.embedding <=> %s::vector AS distance
              FROM news n
              JOIN news_embedding ne ON ne.news_id = n.id
             WHERE n.symbol = %s
               AND n.published_at >= NOW() - (%s || ' days')::interval
             ORDER BY ne.embedding <=> %s::vector
             LIMIT %s
        """
        async with self._pool.connection() as conn:
            await register_vector_async(conn)
            async with conn.cursor() as cur:
                await cur.execute(
                    sql,
                    (
                        query.embedding,
                        query.symbol.value,
                        query.days_window,
                        query.embedding,
                        query.top_k,
                    ),
                )
                rows = await cur.fetchall()
        return [self._row_to_news(r) for r in rows]

    async def upsert_missing_embeddings(
        self,
        symbol: StockSymbol,
        embed_fn: Callable[[list[str]], Awaitable[list[list[float]]]],
        batch_limit: int = 50,
    ) -> int:
        fetch_sql = """
            SELECT n.id, n.title, n.body
              FROM news n
         LEFT JOIN news_embedding ne ON ne.news_id = n.id
             WHERE n.symbol = %s AND ne.news_id IS NULL
             ORDER BY n.published_at DESC
             LIMIT %s
        """
        async with self._pool.connection() as conn:
            async with conn.cursor() as cur:
                await cur.execute(fetch_sql, (symbol.value, batch_limit))
                rows = await cur.fetchall()

        if not rows:
            return 0

        texts = [f"{title}\n{body}" for (_id, title, body) in rows]
        vectors = await embed_fn(texts)

        insert_sql = """
            INSERT INTO news_embedding (news_id, embedding, model)
            VALUES (%s, %s::vector, %s)
            ON CONFLICT (news_id) DO NOTHING
        """
        async with self._pool.connection() as conn:
            await register_vector_async(conn)
            async with conn.cursor() as cur:
                for (news_id, _t, _b), vector in zip(rows, vectors, strict=True):
                    await cur.execute(
                        insert_sql, (news_id, vector, settings.openai_embedding_model)
                    )
            await conn.commit()
        return len(rows)

    @staticmethod
    def _row_to_news(row) -> RetrievedNews:
        return RetrievedNews(
            id=row[0],
            title=row[1],
            body=row[2],
            source=row[3],
            source_url=row[4],
            published_at=(
                row[5] if isinstance(row[5], datetime) else datetime.fromisoformat(str(row[5]))
            ),
            score=float(row[6]),
        )
