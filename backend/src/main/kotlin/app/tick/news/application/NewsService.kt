package app.tick.news.application

import app.tick.common.domain.StockCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class NewsService(
    /**
     * 등록된 모든 collector (네이버 / DART 등) 를 순회. 한 소스가 실패해도 다른 소스는 정상 진행.
     * disabled 어댑터는 자체적으로 emptyList 를 반환하는 게 규약.
     */
    private val collectors: List<NewsCollectorPort>,
    private val loadNews: LoadNewsPort,
    private val saveNews: SaveNewsPort,
    private val archive: NewsArchivePort,
) : CollectNewsForSymbolUseCase, GetRecentNewsUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun collect(stockCode: StockCode, limit: Int): CollectResult {
        var totalFetched = 0
        var totalSaved = 0
        for (collector in collectors) {
            val name = collector.javaClass.simpleName
            val fetched = try {
                collector.search(stockCode, limit)
            } catch (e: Exception) {
                log.warn("collector {} failed for {}: {}", name, stockCode.value, e.message)
                emptyList()
            }
            var savedThisSource = 0
            fetched.forEach { news ->
                if (!loadNews.existsByContentHash(news.contentHash)) {
                    // 아카이빙 결과 URL 을 news 에 세팅한 뒤 저장. NoOp 어댑터면 null 그대로.
                    val archiveUrl = archive.archive(news)
                    saveNews.save(news.withArchiveUrl(archiveUrl))
                    savedThisSource++
                }
            }
            totalFetched += fetched.size
            totalSaved += savedThisSource
            log.info(
                "news collect symbol={} collector={} fetched={} saved={}",
                stockCode.value, name, fetched.size, savedThisSource,
            )
        }
        return CollectResult(fetched = totalFetched, saved = totalSaved)
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
