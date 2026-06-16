"""pgvector 기반 retrieval.

Spring backend 의 News 도메인이 news / news_embedding 테이블에 적재한다.
ai-server 는 같은 DB 를 읽기 + news_embedding 에 임베딩만 추가/조회.
"""

from dataclasses import dataclass
from datetime import datetime

from pgvector.psycopg import register_vector

from app.config import settings
from app.deps import pg_pool


@dataclass
class RelevantNews:
    id: int
    title: str
    body: str
    source: str | None
    source_url: str | None
    published_at: datetime
    distance: float  # cosine distance, 작을수록 유사


def search(symbol: str, query_embedding: list[float], limit: int | None = None) -> list[RelevantNews]:
    """질의 임베딩 ↔ 종목별 뉴스 임베딩 cosine 유사도 top-K.

    최근 N 일 윈도우 내에서만 검색 (오래된 뉴스는 의미 적음).
    """
    k = limit or settings.retrieval_top_k
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
    with pg_pool().connection() as conn:
        register_vector(conn)
        with conn.cursor() as cur:
            cur.execute(
                sql,
                (query_embedding, symbol, settings.retrieval_days_window, query_embedding, k),
            )
            rows = cur.fetchall()
    return [
        RelevantNews(
            id=r[0],
            title=r[1],
            body=r[2],
            source=r[3],
            source_url=r[4],
            published_at=r[5],
            distance=float(r[6]),
        )
        for r in rows
    ]


def upsert_missing_embeddings(symbol: str, batch_limit: int = 50) -> int:
    """이 종목의 news 중 news_embedding 에 아직 없는 행을 찾아 임베딩 생성 + 저장.

    반환: 새로 임베딩한 건수.
    """
    # 1) 임베딩 안 된 뉴스 조회
    fetch_sql = """
        SELECT n.id, n.title, n.body
          FROM news n
     LEFT JOIN news_embedding ne ON ne.news_id = n.id
         WHERE n.symbol = %s AND ne.news_id IS NULL
         ORDER BY n.published_at DESC
         LIMIT %s
    """
    with pg_pool().connection() as conn:
        with conn.cursor() as cur:
            cur.execute(fetch_sql, (symbol, batch_limit))
            rows = cur.fetchall()

    if not rows:
        return 0

    from app.embedding import embed_batch  # avoid circular at module-load

    texts = [f"{title}\n{body}" for (_id, title, body) in rows]
    vectors = embed_batch(texts)

    insert_sql = """
        INSERT INTO news_embedding (news_id, embedding, model)
        VALUES (%s, %s::vector, %s)
        ON CONFLICT (news_id) DO NOTHING
    """
    with pg_pool().connection() as conn:
        register_vector(conn)
        with conn.cursor() as cur:
            for (news_id, _t, _b), vector in zip(rows, vectors, strict=True):
                cur.execute(insert_sql, (news_id, vector, settings.openai_embedding_model))
        conn.commit()
    return len(rows)
