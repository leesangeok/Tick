from app.application.commands.summarize_stock_command import SummarizeStockCommand
from app.application.results.stock_summary_result import StockSummaryResult
from app.config.settings import settings
from app.ports.embedding_port import EmbeddingPort
from app.ports.llm_port import LlmPort
from app.ports.retriever_port import NewsRetrieverPort, RetrievalQuery
from app.ports.trace_port import TraceEvent, TracePort


class SummarizeStockUseCase:
    """RAG 요약 use case.

    1. 검색용 query 를 만든다 → embedding
    2. retriever 로 관련 뉴스 top-K 검색
    3. LLM 으로 요약 생성 (구조화 JSON)
    4. trace 기록
    5. 결과 반환
    """

    def __init__(
        self,
        embedding: EmbeddingPort,
        retriever: NewsRetrieverPort,
        llm: LlmPort,
        trace: TracePort,
    ) -> None:
        self._embedding = embedding
        self._retriever = retriever
        self._llm = llm
        self._trace = trace

    async def execute(self, command: SummarizeStockCommand) -> StockSummaryResult:
        query_text = f"{command.stock_name}({command.symbol.value}) 주가 변동 이유 실적 뉴스"
        query_embedding = await self._embedding.embed(query_text)

        news = await self._retriever.retrieve(
            RetrievalQuery(
                symbol=command.symbol,
                embedding=query_embedding,
                top_k=settings.retrieval_top_k,
                days_window=settings.retrieval_days_window,
            )
        )

        summary = await self._llm.generate_stock_summary(
            symbol=command.symbol,
            stock_name=command.stock_name,
            news=news,
        )

        await self._trace.record(
            TraceEvent(
                name="summary_generated",
                metadata={
                    "symbol": command.symbol.value,
                    "news_count": len(news),
                    "summary_chars": len(summary.summary),
                },
            )
        )

        return StockSummaryResult(summary=summary, retrieved_count=len(news))
