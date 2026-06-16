-- V6 (news.content_hash) + V4 (refresh_token.token_hash) 가 CHAR(64) 로 만들어졌는데
-- JPA Entity 는 String → VARCHAR 매핑이라 schema-validation 실패.
-- VARCHAR(64) 로 통일. 같은 64 자라 데이터 손실 없음 (CHAR 는 right-padded → VARCHAR 변환 시 trailing space 유지).
ALTER TABLE news ALTER COLUMN content_hash TYPE VARCHAR(64);
ALTER TABLE refresh_token ALTER COLUMN token_hash TYPE VARCHAR(64);
