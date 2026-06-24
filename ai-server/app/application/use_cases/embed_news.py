from app.application.commands.embed_news_command import EmbedNewsCommand
from app.application.results.embed_result import EmbedResult
from app.ports.embedding_port import EmbeddingPort
from app.ports.retriever_port import NewsRetrieverPort
from app.ports.summary_cache_port import SummaryCachePort
from app.ports.trace_port import TraceEvent, TracePort


class EmbedNewsUseCase:
    """이 종목의 임베딩 안 된 뉴스에 대해 일괄 임베딩 생성 + 저장.

    새 뉴스가 임베딩 되면 같은 종목의 캐시된 summary 가 stale 이 되므로
    `SummaryCachePort.invalidate(symbol)` 호출. 다음 summary 요청 시 cache miss
    → fresh summary 생성.
    """

    def __init__(
        self,
        embedding: EmbeddingPort,
        retriever: NewsRetrieverPort,
        trace: TracePort,
        cache: SummaryCachePort,
    ) -> None:
        self._embedding = embedding
        self._retriever = retriever
        self._trace = trace
        self._cache = cache

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
            if upserted > 0:
                await self._cache.invalidate(command.symbol)

            await self._trace.record(
                TraceEvent(
                    name="news_embedded",
                    metadata={"symbol": command.symbol.value, "upserted": upserted},
                )
            )
            return EmbedResult(upserted=upserted)
