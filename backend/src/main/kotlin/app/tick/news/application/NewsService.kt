package app.tick.news.application

import app.tick.common.domain.StockCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class NewsService(
    private val collector: NewsCollectorPort,
    private val loadNews: LoadNewsPort,
    private val saveNews: SaveNewsPort,
) : CollectNewsForSymbolUseCase, GetRecentNewsUseCase {

    @Transactional
    override fun collect(stockCode: StockCode, limit: Int): CollectResult {
        val fetched = collector.search(stockCode, limit)
        var saved = 0
        fetched.forEach { news ->
            if (!loadNews.existsByContentHash(news.contentHash)) {
                saveNews.save(news)
                saved++
            }
        }
        return CollectResult(fetched = fetched.size, saved = saved)
    }

    @Transactional(readOnly = true)
    override fun recent(stockCode: StockCode, limit: Int): List<NewsResult> =
        loadNews.loadRecent(stockCode, limit).map { it.toResult() }
}

private val isoFormatter = DateTimeFormatter.ISO_INSTANT

private fun app.tick.news.domain.News.toResult(): NewsResult = NewsResult(
    id = id ?: error("News id is null after persistence"),
    title = title,
    body = body,
    source = source,
    sourceUrl = sourceUrl,
    publishedAt = isoFormatter.format(publishedAt.atZone(ZoneId.of("UTC")).toInstant()),
)
