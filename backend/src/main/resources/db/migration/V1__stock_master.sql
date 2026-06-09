CREATE TABLE stock_master (
    symbol VARCHAR(10) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    market VARCHAR(10) NOT NULL,
    sector VARCHAR(50) NOT NULL,
    base_price INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO stock_master (symbol, name, market, sector, base_price) VALUES
    ('005930', '삼성전자',          'KOSPI', '반도체',  71500),
    ('000660', 'SK하이닉스',        'KOSPI', '반도체',  195000),
    ('035420', 'NAVER',             'KOSPI', '인터넷',  218000),
    ('035720', '카카오',             'KOSPI', '인터넷',  45200),
    ('005380', '현대차',             'KOSPI', '자동차',  248500),
    ('373220', 'LG에너지솔루션',     'KOSPI', '2차전지', 412000),
    ('068270', '셀트리온',           'KOSPI', '바이오',  192800),
    ('012450', '한화에어로스페이스', 'KOSPI', '방산',    287500),
    ('034020', '두산에너빌리티',     'KOSPI', '기계',    19840),
    ('005490', 'POSCO홀딩스',        'KOSPI', '철강',    358000);
