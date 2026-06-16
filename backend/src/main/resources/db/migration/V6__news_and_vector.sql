-- RAG 인프라: 뉴스 본문 + pgvector 임베딩 저장.
-- pgvector extension 은 Postgres 에 미리 설치되어 있어야 한다 (pgvector/pgvector:pg16 이미지).
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE news (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL REFERENCES stock_master(symbol),
    title VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    source VARCHAR(100),
    source_url VARCHAR(1000),
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- title+body 의 SHA-256 hex. 같은 뉴스 중복 수집 방지.
    content_hash CHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_news_symbol_published ON news(symbol, published_at DESC);

-- 임베딩은 news 와 1:1 이지만 별도 테이블로 분리 — 1536 dim float32 ≈ 6KB 라 news row 와 같이 두면 무거움.
-- text-embedding-3-small 기준 1536 차원. 모델 바꿀 때 vector(N) 도 같이 바뀜.
CREATE TABLE news_embedding (
    news_id BIGINT PRIMARY KEY REFERENCES news(id) ON DELETE CASCADE,
    embedding vector(1536) NOT NULL,
    model VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 데이터 양이 적은 동안엔 linear search 로 충분. 수만 건 넘어가면 HNSW 인덱스 추가:
--   CREATE INDEX ON news_embedding USING hnsw (embedding vector_cosine_ops);
