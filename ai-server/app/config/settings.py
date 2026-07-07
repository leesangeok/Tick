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

    # Cohere Rerank — 비면 rerank 스킵 (RRF 순위 그대로 사용).
    cohere_api_key: str = ""
    cohere_rerank_model: str = "rerank-v3.5"

    # LLM-as-a-judge 를 N회 병렬 호출해 지표별 median 을 사용. 홀수 권장.
    # 1 이면 단일 호출 (하위호환). 3 이면 outlier 완화 + 비용 3배.
    judge_repeat: int = 3
    # 판정 편차 (표준편차) 가 threshold 를 넘으면 자동 재판정 (extras 회 추가 → total 홀수 유지).
    # 판정 3회로도 stochasticity 가 안 잡히는 종목만 선택적으로 재판정. 0 이면 재판정 비활성.
    judge_std_threshold_grounded: float = 0.15
    judge_std_threshold_halluc: float = 1.0
    judge_retry_extras: int = 4

    # 다중 judge (판정 편향 교차검증). True 면 Sonnet 4.6 + Opus 4.7 두 세대의 Claude 로 병렬
    # 판정 후 지표별 median-of-medians. 두 모델 간 판정 편차 (disagreement σ) 도 함께 기록.
    # OpenAI 는 이 프로젝트에서 embedding 전용이라 chat judge 로는 사용 X.
    judge_multi_enabled: bool = False
    opus_judge_model: str = "claude-opus-4-7"

    # Redis — summary 응답 캐시. host 비어있으면 NoOp 으로 자동 fallback.
    redis_host: str = ""
    redis_port: int = 6379
    redis_password: str = ""
    # summary 캐시 TTL (초). 짧으면 fresh, 길면 절감 큼. embed_news 시 자동 invalidate 됨.
    summary_cache_ttl_sec: int = 900  # 15분

    log_level: str = "INFO"


settings = Settings()
