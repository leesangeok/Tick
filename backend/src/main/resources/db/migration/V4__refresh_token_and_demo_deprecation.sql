-- Refresh token 저장소 (Phase 4 Redis 이전 전까지 Postgres 에 보관).
-- raw token 은 client cookie 에만, DB 에는 SHA-256 hash 만 저장한다.
CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_refresh_token_member ON refresh_token(member_id);
CREATE INDEX idx_refresh_token_expires ON refresh_token(expires_at);

-- demo 계정 폐기. V2 에서 시드한 external_id='demo' 행 (V3 에서 member_id NULL 로 남김).
-- ON DELETE CASCADE 로 holding / order_history / deposit_history 자동 정리.
DELETE FROM account WHERE id = 1 AND member_id IS NULL;
