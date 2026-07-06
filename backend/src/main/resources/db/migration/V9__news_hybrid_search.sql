-- Hybrid retrieval 지원. news 에 title+body 기반 tsvector 를 생성 컬럼으로 두고 GIN 인덱스.
-- 한국어 형태소는 안 씀 (simple config) — sparse 의 목적은 숫자/영문/고유명사 정확 매칭 보강.
-- 한국어 어절/n-gram 정밀도가 필요해지면 후속에 pg_bigm 도입.

ALTER TABLE news
    ADD COLUMN title_body_tsv tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(body, '')), 'B')
    ) STORED;

CREATE INDEX idx_news_title_body_tsv ON news USING GIN (title_body_tsv);
