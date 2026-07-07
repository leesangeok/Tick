from app.application.commands.summarize_stock_command import SummarizeStockCommand
from app.application.results.stock_summary_result import StockSummaryResult
from app.config.settings import settings
from app.domain.models.retrieved_news import RetrievedNews
from app.ports.embedding_port import EmbeddingPort
from app.ports.llm_port import LlmPort
from app.ports.query_rewrite_port import QueryRewritePort
from app.ports.retriever_port import NewsRetrieverPort, RetrievalQuery
from app.ports.summary_cache_port import SummaryCachePort
from app.ports.trace_port import TraceEvent, TracePort

# variant 간 RRF 병합 상수. hybrid retriever 내부의 RRF (K=60) 와 별개 계층.
_VARIANT_RRF_K = 60


class SummarizeStockUseCase:
    """RAG 요약 use case.

    0. cache hit 면 즉시 반환 (LLM 호출 0회)
    1. 검색용 query 를 만든다 → embedding
    2. retriever 로 관련 뉴스 top-K 검색
    3. LLM 으로 요약 생성 (구조화 JSON)
    4. cache.set + trace 기록
    5. 결과 반환

    cache 무효화는 `EmbedNewsUseCase` 가 새 뉴스 임베딩 후 자동 invalidate 한다.
    """

    def __init__(
        self,
        embedding: EmbeddingPort,
        retriever: NewsRetrieverPort,
        llm: LlmPort,
        trace: TracePort,
        cache: SummaryCachePort,
        query_rewrite: QueryRewritePort,
    ) -> None:
        self._embedding = embedding
        self._retriever = retriever
        self._llm = llm
        self._trace = trace
        self._cache = cache
        self._query_rewrite = query_rewrite

    async def execute(self, command: SummarizeStockCommand) -> StockSummaryResult:
        async with self._trace.span(
            "summarize_stock",
            symbol=command.symbol.value,
            stock_name=command.stock_name,
        ):
            cached = await self._cache.get(command.symbol)
            if cached is not None:
                await self._trace.record(
                    TraceEvent(
                        name="summary_cache_hit",
                        metadata={"symbol": command.symbol.value},
                    )
                )
                return cached

            base_query = (
                f"{command.stock_name}({command.symbol.value}) 주가 변동 이유 실적 뉴스"
            )
            variants = await self._query_rewrite.rewrite(
                command.symbol, command.stock_name, base_query
            )

            news = await self._retrieve_multi(command, variants)

            summary = await self._llm.generate_stock_summary(
                symbol=command.symbol,
                stock_name=command.stock_name,
                news=news,
            )

            result = StockSummaryResult(summary=summary, retrieved_count=len(news))
            await self._cache.set(command.symbol, result)

            await self._trace.record(
                TraceEvent(
                    name="summary_generated",
                    metadata={
                        "symbol": command.symbol.value,
                        "news_count": len(news),
                        "summary_chars": len(summary.summary),
                        "query_variants": len(variants),
                    },
                )
            )

            return result

    async def _retrieve_multi(
        self, command: SummarizeStockCommand, variants: list[str]
    ) -> list[RetrievedNews]:
        """variant 별로 retrieve 한 뒤 variant 간 RRF 로 병합해 top_k 로 자른다.

        hybrid retriever 는 내부에서 dense+sparse RRF (K=60) 를 이미 하므로, 이 층은 그 위에
        variant 축으로 한 번 더 RRF. 나이 감쇠는 하위 계층에 위임.
        """
        # 단일 variant 는 기존 경로 그대로 (오버헤드 0).
        if len(variants) == 1:
            embedding = await self._embedding.embed(variants[0])
            return await self._retriever.retrieve(
                RetrievalQuery(
                    symbol=command.symbol,
                    embedding=embedding,
                    top_k=settings.retrieval_top_k,
                    days_window=settings.retrieval_days_window,
                    raw_query_text=variants[0],
                )
            )

        # variant 별 병렬 embedding + retrieve 는 순차로 둔다 — API rate 및 DB pool 보호.
        # 나중에 필요하면 asyncio.gather 로 전환 가능.
        per_variant: list[list[RetrievedNews]] = []
        for text in variants:
            embedding = await self._embedding.embed(text)
            per_variant.append(
                await self._retriever.retrieve(
                    RetrievalQuery(
                        symbol=command.symbol,
                        embedding=embedding,
                        top_k=settings.retrieval_top_k,
                        days_window=settings.retrieval_days_window,
                        raw_query_text=text,
                    )
                )
            )

        return self._variant_rrf_merge(per_variant, settings.retrieval_top_k)

    @staticmethod
    def _variant_rrf_merge(
        per_variant: list[list[RetrievedNews]], top_k: int
    ) -> list[RetrievedNews]:
        agg: dict[int, tuple[RetrievedNews, float]] = {}
        for items in per_variant:
            for rank, item in enumerate(items):
                prev = agg.get(item.id)
                score = (prev[1] if prev else 0.0) + 1.0 / (_VARIANT_RRF_K + rank)
                keep = prev[0] if prev else item
                agg[item.id] = (keep, score)
        merged = [item for item, _ in sorted(agg.values(), key=lambda kv: -kv[1])]
        return merged[:top_k]
