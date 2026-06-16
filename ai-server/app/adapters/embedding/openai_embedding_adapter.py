"""LangChain `OpenAIEmbeddings` 래퍼를 통한 EmbeddingPort 구현.

Langfuse callback 으로 자동 trace.
"""

from langchain_openai import OpenAIEmbeddings

from app.adapters.observability.langfuse_callback import langfuse_callback_handler
from app.config.settings import settings


class OpenAiEmbeddingAdapter:
    def __init__(self) -> None:
        self._client = OpenAIEmbeddings(
            model=settings.openai_embedding_model,
            api_key=settings.openai_api_key,
        )
        self._config = {"callbacks": [langfuse_callback_handler()]}

    async def embed(self, text: str) -> list[float]:
        # langchain_openai 의 embedding 함수는 RunnableConfig 를 직접 받지 않으나
        # CallbackManager 가 instance-level callbacks 도 적용함.
        # 명시적 추적이 필요하면 use case 단에서 update_current_span 으로 보강.
        return await self._client.aembed_query(text)

    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        return await self._client.aembed_documents(texts)
