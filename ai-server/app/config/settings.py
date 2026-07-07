from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Postgres (pgvector enabled). backend 와 같은 DB.
    postgres_dsn: str = "postgresql://tick:tick@postgres:5432/tick"

    # OpenAI - embedding only
    openai_api_key: str = ""
    openai_embedding_model: str = "text-embedding-3-small"
    openai_embedding_dim: int = 1536

    # Anthropic - LLM
    anthropic_api_key: str = ""
    anthropic_model: str = "claude-haiku-4-5"
    anthropic_max_tokens: int = 800

    # Langfuse - 다음 PR 에서 활성화. 일단 키 자리만.
    langfuse_public_key: str = ""
    langfuse_secret_key: str = ""
    langfuse_host: str = "https://cloud.langfuse.com"

    # RAG
    retrieval_top_k: int = 5
    retrieval_days_window: int = 14
    # hybrid 검색에서 dense/sparse 각각의 초기 후보 개수. rerank 는 이걸 병합해 top_k 로 좁힌다.
    retrieval_initial_k: int = 20
    # RRF 병합 점수에 published_at 기반 지수 감쇠 (반감기 = N 일). 0 이면 비활성.
    # 급등락 이유 요약은 최신 뉴스가 중요. dense/sparse rank 는 시간 무관이라 결합해 신선도 보정.
    retrieval_freshness_half_life_days: float = 3.0

    # 쿼리 재작성 — Haiku 로 base_query 를 다변량 쿼리로 확장 후 variant별 hybrid 검색 → RRF 병합.
    # 종목별 이슈 (실적/이벤트/섹터) 를 다양한 관점에서 잡아 recall 향상.
    # 실패 시 base_query 로 fallback.
    query_rewrite_enabled: bool = False
    query_rewrite_model: str = "claude-haiku-4-5"

    # Cohere Rerank — 비면 rerank 스킵 (RRF 순위 그대로 사용).
    cohere_api_key: str = ""
    cohere_rerank_model: str = "rerank-v3.5"

    # ---- Multi-judge / eval 설정은 evals/eval_settings.py 로 이동. ----
    # 이유: prod 요약 use case 가 실수로 참조하면 요청당 최대 7회 LLM 판정 호출로 비용 폭발.
    # eval 파이프라인만 참조하도록 물리적 격리.

    # Redis — summary 응답 캐시. host 비어있으면 NoOp 으로 자동 fallback.
    redis_host: str = ""
    redis_port: int = 6379
    redis_password: str = ""
    # summary 캐시 TTL (초). 짧으면 fresh, 길면 절감 큼. embed_news 시 자동 invalidate 됨.
    summary_cache_ttl_sec: int = 900  # 15분

    log_level: str = "INFO"


settings = Settings()
