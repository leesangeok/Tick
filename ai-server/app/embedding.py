"""OpenAI text embedding."""

from app.config import settings
from app.deps import openai_client


def embed(text: str) -> list[float]:
    """단일 텍스트 → 1536 dim 임베딩 (text-embedding-3-small)."""
    response = openai_client().embeddings.create(
        model=settings.openai_embedding_model,
        input=text,
    )
    return response.data[0].embedding


def embed_batch(texts: list[str]) -> list[list[float]]:
    """배치 임베딩 — OpenAI 는 한 요청에 여러 입력 OK (요금 동일)."""
    if not texts:
        return []
    response = openai_client().embeddings.create(
        model=settings.openai_embedding_model,
        input=texts,
    )
    # OpenAI 는 입력 순서대로 응답 보장
    return [d.embedding for d in response.data]
