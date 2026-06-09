CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(50) NOT NULL UNIQUE,
    cash BIGINT NOT NULL DEFAULT 0,
    total_deposits BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE holding (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL REFERENCES stock_master(symbol),
    quantity INTEGER NOT NULL,
    average_price INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_id, symbol)
);
CREATE INDEX idx_holding_account ON holding(account_id);

CREATE TABLE order_history (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL REFERENCES stock_master(symbol),
    side VARCHAR(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    order_type VARCHAR(8) NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT')),
    quantity INTEGER NOT NULL,
    price INTEGER NOT NULL,
    filled_quantity INTEGER,
    status VARCHAR(10) NOT NULL CHECK (status IN ('PENDING', 'FILLED', 'CANCELED', 'REJECTED')),
    average_cost_at INTEGER,
    realized_profit_loss BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filled_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_order_account_created ON order_history(account_id, created_at DESC);

CREATE TABLE deposit_history (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    amount BIGINT NOT NULL,
    type VARCHAR(8) NOT NULL DEFAULT 'DEPOSIT' CHECK (type IN ('DEPOSIT', 'WITHDRAW')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_deposit_account_created ON deposit_history(account_id, created_at DESC);

-- Seed demo account matching current mock state
INSERT INTO account (id, external_id, cash, total_deposits, created_at)
VALUES (1, 'demo', 13010000, 25000000, '2026-05-01 09:00:00+09');
SELECT setval('account_id_seq', 1, true);

INSERT INTO holding (account_id, symbol, quantity, average_price) VALUES
(1, '005930', 50, 68000),
(1, '000660', 12, 180000),
(1, '373220', 5, 450000),
(1, '068270', 20, 175000),
(1, '005380', 8, 240000);

INSERT INTO order_history (account_id, symbol, side, order_type, quantity, price, filled_quantity, status, average_cost_at, realized_profit_loss, created_at, filled_at) VALUES
(1, '005930', 'BUY',  'LIMIT',  10,  71200, 10,   'FILLED',   NULL,   NULL,    '2026-06-08 09:32:00+09', '2026-06-08 09:32:14+09'),
(1, '000660', 'BUY',  'MARKET',  5, 195800,  5,   'FILLED',   NULL,   NULL,    '2026-06-08 10:14:00+09', '2026-06-08 10:14:01+09'),
(1, '035720', 'SELL', 'LIMIT',  20,  46000, NULL, 'PENDING',  NULL,   NULL,    '2026-06-08 11:02:00+09', NULL),
(1, '373220', 'BUY',  'LIMIT',   2, 405000, NULL, 'CANCELED', NULL,   NULL,    '2026-06-07 14:20:00+09', NULL),
(1, '068270', 'BUY',  'MARKET', 10, 192800, 10,   'FILLED',   NULL,   NULL,    '2026-06-07 13:45:00+09', '2026-06-07 13:45:00+09'),
(1, '005380', 'SELL', 'LIMIT',   3, 252000, NULL, 'PENDING',  NULL,   NULL,    '2026-06-06 15:10:00+09', NULL),
(1, '068270', 'SELL', 'LIMIT',  10, 188400, 10,   'FILLED',   170000, 184000,  '2026-06-05 14:55:00+09', '2026-06-05 15:02:00+09'),
(1, '005490', 'SELL', 'MARKET',  1, 380000,  1,   'FILLED',   320000,  60000,  '2026-06-03 13:15:00+09', '2026-06-03 13:15:00+09'),
(1, '012450', 'SELL', 'LIMIT',   5, 285000,  5,   'FILLED',   240000, 225000,  '2026-05-28 10:42:00+09', '2026-05-28 10:42:30+09'),
(1, '034020', 'SELL', 'LIMIT', 100,  21000, 100,  'FILLED',    15000, 600000,  '2026-05-21 14:08:00+09', '2026-05-21 14:08:01+09'),
(1, '035720', 'SELL', 'LIMIT',  30,  48200,  30,  'FILLED',    42500, 171000,  '2026-05-12 11:25:00+09', '2026-05-12 11:25:18+09');

INSERT INTO deposit_history (account_id, amount, type, created_at) VALUES
(1, 25000000, 'DEPOSIT', '2026-05-01 09:00:00+09');
