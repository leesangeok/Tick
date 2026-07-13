package app.tick.ai.adapter.scheduler

import app.tick.ai.application.EmbedStockNewsUseCase
import app.tick.watchlist.application.GetWatchedSymbolsUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 뉴스 수집 스케줄러가 저장해둔 새 뉴스에 대해 임베딩 upsert 를 야간에 일괄 수행. RAG 검색 품질을
 * 유지하기 위해 필요. Backend 는 요청만 하고 실제 임베딩/DB write 는 ai-server 가 수행.
 *
 * 뉴스 수집 다음 시각에 돌게 cron 을 설정. 임베딩은 결과적 일관성이라 지연 몇 시간은 허용.
 */
@Component
@ConditionalOnProperty(
    name = ["tick.schedule.ai-embedding.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class AiEmbeddingScheduler(
    private val embedNews: EmbedStockNewsUseCase,
    private val watchedSymbols: GetWatchedSymbolsUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${tick.schedule.ai-embedding.cron}", zone = "Asia/Seoul")
    fun run() {
        val symbols = watchedSymbols.allDistinctSymbols()
        if (symbols.isEmpty()) {
            log.info("ai-embedding scheduler: no watched symbols, skip")
            return
        }
        var totalUpserted = 0
        var failed = 0
        for (code in symbols) {
            try {
                val result = embedNews.embed(code)
                totalUpserted += result.upserted
            } catch (e: Exception) {
                // ai-server 다운/타임아웃 등. 한 종목 실패로 다음 종목까지 막지 않는다.
                failed++
                log.warn("ai-embedding symbol={} failed: {}", code.value, e.message)
            }
        }
        log.info(
            "ai-embedding done: symbols={} upserted={} failed={}",
            symbols.size, totalUpserted, failed,
        )
    }
}
