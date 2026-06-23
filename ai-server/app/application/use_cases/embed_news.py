from app.application.commands.embed_news_command import EmbedNewsCommand
from app.application.results.embed_result import EmbedResult
from app.ports.embedding_port import EmbeddingPort
from app.ports.retriever_port import NewsRetrieverPort
from app.ports.trace_port import TraceEvent, TracePort


class EmbedNewsUseCase:
    """이 종목의 임베딩 안 된 뉴스에 대해 일괄 임베딩 생성 + 저장."""

    def __init__(
        self,
        embedding: EmbeddingPort,
        retriever: NewsRetrieverPort,
        trace: TracePort,
    ) -> None:
        self._embedding = embedding
        self._retriever = retriever
        self._trace = trace

    async def execute(self, command: EmbedNewsCommand) -> EmbedResult:
        async with self._trace.span(
            "embed_news",
            symbol=command.symbol.value,
            batch_limit=command.batch_limit,
        ):
            upserted = await self._retriever.upsert_missing_embeddings(
                symbol=command.symbol,
                embed_fn=self._embedding.embed_batch,
                batch_limit=command.batch_limit,
            )
            await self._trace.record(
                TraceEvent(
                    name="news_embedded",
                    metadata={"symbol": command.symbol.value, "upserted": upserted},
                )
            )
            return EmbedResult(upserted=upserted)
