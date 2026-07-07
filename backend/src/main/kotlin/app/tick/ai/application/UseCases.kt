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
    val keyReasons: List<KeyReason>,
    val riskNotes: List<String>,
    val sources: List<SummarySource>,
    val retrievedCount: Int,
)

/**
 * 근거 문장 + 뒷받침 뉴스 인덱스 (1-based, sources 배열 인덱스).
 * 프론트가 `sources[i-1]` 로 lookup 하여 인용/하이라이트.
 */
data class KeyReason(
    val text: String,
    val sourceIndices: List<Int>,
)

data class SummarySource(
    val newsId: Long,
    val title: String,
    val source: String?,
    val sourceUrl: String?,
    val publishedAt: String,
)

data class EmbedResult(val upserted: Int)
