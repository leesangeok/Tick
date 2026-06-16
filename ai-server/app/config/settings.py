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

    log_level: str = "INFO"


settings = Settings()
