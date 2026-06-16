package app.tick.news.application

import app.tick.common.domain.StockCode

interface CollectNewsForSymbolUseCase {
    fun collect(stockCode: StockCode, limit: Int = 20): CollectResult
}

interface GetRecentNewsUseCase {
    fun recent(stockCode: StockCode, limit: Int = 10): List<NewsResult>
}

data class CollectResult(val fetched: Int, val saved: Int)

data class NewsResult(
    val id: Long,
    val title: String,
    val body: String,
    val source: String?,
    val sourceUrl: String?,
    val publishedAt: String,
)
