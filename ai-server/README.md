# Tick AI Server

Spring backend 가 내부 docker network 로 호출하는 RAG 서버. 헥사고날 구조 + LangChain.

## 스택
- Python 3.12 + FastAPI + uvicorn (async)
- pgvector (Spring backend 와 같은 Postgres 공유)
- LangChain: `langchain-openai` (embedding), `langchain-anthropic` (LLM)
- OpenAI `text-embedding-3-small` (1536 dim — V6 schema 일치)
- Anthropic Claude (`claude-haiku-4-5`, prompt cache)

## 디렉토리
```
app/
  api/{routes,schemas}/      # HTTP 진입
  application/
    use_cases/               # SummarizeStockUseCase, EmbedNewsUseCase
    commands/, results/, prompts/
  domain/
    models/, value_objects/  # AiSummary, RetrievedNews, StockSymbol
  ports/                     # EmbeddingPort, LlmPort, NewsRetrieverPort, TracePort
  adapters/
    embedding/openai_*       # LangChain OpenAIEmbeddings wrapper
    llm/anthropic_*          # LangChain ChatAnthropic + prompt cache
    retriever/pgvector_*     # 직접 psycopg async + pgvector
    observability/noop_*     # Langfuse 는 다음 PR
  config/settings.py
  deps.py                    # DI wiring (port → adapter)
  main.py                    # FastAPI lifespan
```

## 엔드포인트
- `GET  /health`
- `POST /ai/embeddings/{symbol}` — 임베딩 안 된 뉴스 일괄 임베딩
- `POST /ai/summary` — `{symbol, stock_name}` → `{summary, key_reasons, risk_notes, sources, retrieved_count}`

## 환경변수
```
POSTGRES_DSN=postgresql://tick:tick@postgres:5432/tick
OPENAI_API_KEY=sk-proj-...
ANTHROPIC_API_KEY=sk-ant-api03-...
# Langfuse (다음 PR)
LANGFUSE_PUBLIC_KEY=pk-lf-...
LANGFUSE_SECRET_KEY=sk-lf-...
LANGFUSE_HOST=https://cloud.langfuse.com
```

## 로컬 실행
```bash
uv sync
uv run uvicorn app.main:app --reload
```

## 보안
외부 노출 X. Caddy 라우팅 없음. Spring backend 만 docker network 내부에서 호출.

## 후속
- PR β: Langfuse trace adapter (`langfuse` SDK + LangChain CallbackHandler)
- PR γ: LangGraph workflow (retrieve → build prompt → llm → finalize)
