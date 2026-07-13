"""Hybrid retriever = dense (pgvector cosine) + sparse (Postgres FTS) → RRF 병합 → reranker.

이유:
- dense 는 뉴스 자연어 body 와 쿼리 임베딩의 의미 유사도를 잡지만, 짧고 형식적인 DART 공시나
  숫자/고유명사 정확 매칭에는 약하다.
- sparse (Postgres tsvector + ts_rank_cd) 는 정확 매칭/희귀 term 에 강하다.
- 두 랭킹을 Reciprocal Rank Fusion (RRF, 표준 상수 K=60) 으로 병합해 상호 보완.
- 병합 결과 상위 N (기본 3 * top_k) 을 reranker 로 최종 정렬해 요약 품질 향상.

upsert_missing_embeddings 는 원본 pgvector 어댑터에 위임 — 이 어댑터의 역할은 retrieve 뿐.
"""

import math
from collections.abc import Awaitable, Callable
from datetime import UTC, datetime

from pgvector.psycopg import register_vector_async
from psycopg_pool import AsyncConnectionPool

from app.adapters.retriever.pgvector_retriever_adapter import PgvectorNewsRetrieverAdapter
from app.config.settings import settings
from app.domain.models.retrieved_news import RetrievedNews
from app.domain.value_objects.stock_symbol import StockSymbol
from app.ports.reranker_port import RerankerPort
from app.ports.retriever_port import RetrievalQuery

RRF_K = 60


class HybridNewsRetrieverAdapter:
    def __init__(
        self,
        pool: AsyncConnectionPool,
        reranker: RerankerPort | None,
        embedding_delegate: PgvectorNewsRetrieverAdapter,
    ) -> None:
        self._pool = pool
        self._reranker = reranker
        self._delegate = embedding_delegate
        self._initial_k = settings.retrieval_initial_k
        self._freshness_half_life_days = settings.retrieval_freshness_half_life_days

    async def retrieve(self, query: RetrievalQuery) -> list[RetrievedNews]:
        dense = await self._dense_retrieve(query)
        sparse = await self._sparse_retrieve(query)
        fused = self._rrf_merge(dense, sparse)

        # reranker 후보 폭 — top_k 의 3배 정도가 실무 관례. 소음은 reranker 가 걸러줌.
        candidate_count = min(len(fused), max(query.top_k * 3, query.top_k))
        candidates = fused[:candidate_count]

        if self._reranker is None or not candidates or not query.raw_query_text:
            return candidates[: query.top_k]

        try:
            return await self._reranker.rerank(
                query.raw_query_text, candidates, top_n=query.top_k
            )
        except Exception:
            # rerank 실패 시 RRF 순위로 fallback — 회복 가능한 저품질보다 조용한 성공이 낫다.
            return candidates[: query.top_k]

    async def upsert_missing_embeddings(
        self,
        symbol: StockSymbol,
        embed_fn: Callable[[list[str]], Awaitable[list[list[float]]]],
        batch_limit: int = 50,
    ) -> int:
        return await self._delegate.upsert_missing_embeddings(symbol, embed_fn, batch_limit)

    async def _dense_retrieve(self, query: RetrievalQuery) -> list[RetrievedNews]:
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
                        self._initial_k,
                    ),
                )
                rows = await cur.fetchall()
        return [self._row_to_news(r) for r in rows]

    async def _sparse_retrieve(self, query: RetrievalQuery) -> list[RetrievedNews]:
        if not query.raw_query_text:
            return []
        sql = """
            SELECT n.id, n.title, n.body, n.source, n.source_url, n.published_at,
                   ts_rank_cd(n.title_body_tsv, plainto_tsquery('simple', %s)) AS score
              FROM news n
             WHERE n.symbol = %s
               AND n.published_at >= NOW() - (%s || ' days')::interval
               AND n.title_body_tsv @@ plainto_tsquery('simple', %s)
             ORDER BY score DESC
             LIMIT %s
        """
        async with self._pool.connection() as conn:
            async with conn.cursor() as cur:
                await cur.execute(
                    sql,
                    (
                        query.raw_query_text,
                        query.symbol.value,
                        query.days_window,
                        query.raw_query_text,
                        self._initial_k,
                    ),
                )
                rows = await cur.fetchall()
        # sparse score 는 rank 로 쓸 뿐이라 distance 자리에 그대로 넣어 두어도 무해하지만,
        # 의미를 보존하기 위해 음수로 뒤집어 넣는다 (거리 개념 유지).
        return [self._row_to_news(r, invert_score=True) for r in rows]

    def _rrf_merge(
        self, dense: list[RetrievedNews], sparse: list[RetrievedNews]
    ) -> list[RetrievedNews]:
        agg: dict[int, tuple[RetrievedNews, float]] = {}
        for rank, item in enumerate(dense):
            prev = agg.get(item.id)
            score = (prev[1] if prev else 0.0) + 1.0 / (RRF_K + rank)
            agg[item.id] = (item, score)
        for rank, item in enumerate(sparse):
            prev = agg.get(item.id)
            score = (prev[1] if prev else 0.0) + 1.0 / (RRF_K + rank)
            # 같은 id 라도 dense 쪽 인스턴스를 유지 (score 필드 일관성).
            keep = prev[0] if prev else item
            agg[item.id] = (keep, score)
        self._apply_freshness_decay(agg)
        # RRF 점수 내림차순
        return [item for item, _ in sorted(agg.values(), key=lambda kv: -kv[1])]

    def _apply_freshness_decay(
        self, agg: dict[int, tuple[RetrievedNews, float]]
    ) -> None:
        # 반감기(half-life) 의미: age = N 일 지나면 점수가 정확히 절반.
        # RRF 만으로는 14일 윈도우 내에서 최신뉴스 우선 순위가 약해서, 급등락 이유 요약이
        # 오래된 뉴스에 편향되는 문제를 완화한다.
        if self._freshness_half_life_days <= 0:
            return
        now = datetime.now(UTC)
        for news_id, (item, score) in agg.items():
            published = item.published_at
            if published.tzinfo is None:
                published = published.replace(tzinfo=UTC)
            age_days = max(0.0, (now - published).total_seconds() / 86400.0)
            decay = math.pow(2.0, -age_days / self._freshness_half_life_days)
            agg[news_id] = (item, score * decay)

    @staticmethod
    def _row_to_news(row, invert_score: bool = False) -> RetrievedNews:
        score = float(row[6])
        if invert_score:
            score = -score
        return RetrievedNews(
            id=row[0],
            title=row[1],
            body=row[2],
            source=row[3],
            source_url=row[4],
            published_at=row[5]
            if isinstance(row[5], datetime)
            else datetime.fromisoformat(str(row[5])),
            score=score,
        )
