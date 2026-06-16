"""LangChain `OpenAIEmbeddings` 래퍼를 통한 EmbeddingPort 구현."""

from langchain_openai import OpenAIEmbeddings

from app.config.settings import settings


class OpenAiEmbeddingAdapter:
    def __init__(self) -> None:
        self._client = OpenAIEmbeddings(
            model=settings.openai_embedding_model,
            api_key=settings.openai_api_key,
        )

    async def embed(self, text: str) -> list[float]:
        return await self._client.aembed_query(text)

    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        return await self._client.aembed_documents(texts)
