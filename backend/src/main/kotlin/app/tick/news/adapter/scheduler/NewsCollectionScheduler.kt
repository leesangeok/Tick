package app.tick.news.adapter.scheduler

import app.tick.news.application.CollectNewsForSymbolUseCase
import app.tick.watchlist.application.GetWatchedSymbolsUseCase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 사용자 watchlist 에 등록된 종목만 주기적으로 뉴스 수집. StockMaster 전체를 도는 대신 관심 대상만
 * 스캔해 외부 API rate limit (Naver / DART) 을 아낀다. watchlist 가 비면 no-op 이라 신규 배포 후에도
 * 안전.
 *
 * enabled 기본 false — 로컬 개발 중 실수로 외부 API 를 두드리지 않도록 opt-in.
 */
@Component
@ConditionalOnProperty(
    name = ["tick.schedule.news-collect.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class NewsCollectionScheduler(
    private val collectNews: CollectNewsForSymbolUseCase,
    private val watchedSymbols: GetWatchedSymbolsUseCase,
    @Value("\${tick.schedule.news-collect.per-symbol-limit:20}") private val perSymbolLimit: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${tick.schedule.news-collect.cron}", zone = "Asia/Seoul")
    fun run() {
        val symbols = watchedSymbols.allDistinctSymbols()
        if (symbols.isEmpty()) {
            log.info("news-collect scheduler: no watched symbols, skip")
            return
        }
        var totalFetched = 0
        var totalSaved = 0
        var failed = 0
        for (code in symbols) {
            try {
                val result = collectNews.collect(code, perSymbolLimit)
                totalFetched += result.fetched
                totalSaved += result.saved
            } catch (e: Exception) {
                // 한 종목 실패가 전체 배치를 막지 않도록 격리. Naver / DART 어느 한쪽 장애 시 자주 발생.
                failed++
                log.warn("news-collect symbol={} failed: {}", code.value, e.message)
            }
        }
        log.info(
            "news-collect done: symbols={} fetched={} saved={} failed={}",
            symbols.size, totalFetched, totalSaved, failed,
        )
    }
}
