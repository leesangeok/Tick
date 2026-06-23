package app.tick.ai.application

import app.tick.common.domain.StockCode

interface GetAiSummaryUseCase {
    fun summary(stockCode: StockCode): AiSummaryResult
}

interface EmbedStockNewsUseCase {
    fun embed(stockCode: StockCode): EmbedResult
}

data class AiSummaryResult(
    val symbol: String,
    val summary: String,
    val keyReasons: List<String>,
    val riskNotes: List<String>,
    val sources: List<SummarySource>,
    val retrievedCount: Int,
)

data class SummarySource(
    val title: String,
    val source: String?,
    val sourceUrl: String?,
    val publishedAt: String,
)

data class EmbedResult(val upserted: Int)
