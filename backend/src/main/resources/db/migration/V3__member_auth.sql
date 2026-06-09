-- 카카오 OAuth 사용자 (Member)
CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    kakao_id BIGINT NOT NULL UNIQUE,
    email VARCHAR(255),
    nickname VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_member_kakao_id ON member(kakao_id);

-- account 에 member_id FK 추가. 기존 'demo' 계정은 NULL 유지.
ALTER TABLE account
    ADD COLUMN member_id BIGINT REFERENCES member(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uq_account_member_id ON account(member_id) WHERE member_id IS NOT NULL;
CREATE INDEX idx_account_member_id ON account(member_id);
