# Tick AI Server

Spring backend 가 내부 docker network 로 호출하는 RAG 서버.

## 스택
- Python 3.12 + FastAPI + uvicorn
- pgvector (Spring 과 같은 Postgres 공유)
- OpenAI text-embedding-3-small (1536 dim)
- Anthropic Claude (claude-haiku-4-5, prompt caching)

## 엔드포인트
- `GET  /health`
- `POST /ai/embeddings/{symbol}` — 이 종목의 임베딩 안 된 뉴스 일괄 임베딩
- `POST /ai/summary` — `{symbol, stock_name}` → `{summary, evidences[]}`

## 환경변수
```
POSTGRES_DSN=postgresql://tick:tick@postgres:5432/tick
OPENAI_API_KEY=sk-proj-...
ANTHROPIC_API_KEY=sk-ant-api03-...
```

## 로컬 실행
```bash
uv sync
uv run uvicorn app.main:app --reload
```

## 보안
외부 노출 X. Caddy 에서 ai-server 라우팅 없음. Spring backend 만 docker network 내부에서 호출.
