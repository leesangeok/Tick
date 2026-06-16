from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Postgres (pgvector enabled)
    postgres_dsn: str = "postgresql://tick:tick@postgres:5432/tick"

    # OpenAI - embedding only
    openai_api_key: str = ""
    openai_embedding_model: str = "text-embedding-3-small"  # 1536 dim, matches V6 schema

    # Anthropic - LLM
    anthropic_api_key: str = ""
    anthropic_model: str = "claude-haiku-4-5"  # 빠르고 저렴, 요약엔 충분
    anthropic_max_tokens: int = 800

    # RAG
    retrieval_top_k: int = 5
    retrieval_days_window: int = 14

    # Server
    log_level: str = "INFO"


settings = Settings()
