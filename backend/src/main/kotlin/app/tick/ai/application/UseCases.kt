package app.tick.ai.application

import app.tick.common.domain.StockCode

interface GetAiSummaryUseCase {
    fun summary(stockCode: StockCode): AiSummaryResult
}

interface EmbedStockNewsUseCase {
    fun embed(stockCode: StockCode): EmbedResult
}

data class AiSummaryResult(
    val summary: String,
    val evidences: List<Evidence>,
)

data class Evidence(
    val title: String,
    val source: String?,
    val sourceUrl: String?,
    val publishedAt: String,
)

data class EmbedResult(val upserted: Int)
