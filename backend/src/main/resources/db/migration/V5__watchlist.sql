CREATE TABLE watchlist (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL REFERENCES stock_master(symbol),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (member_id, symbol)
);
CREATE INDEX idx_watchlist_member ON watchlist(member_id);
