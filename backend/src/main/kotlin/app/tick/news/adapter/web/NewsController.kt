package app.tick.news.adapter.web

import app.tick.common.domain.StockCode
import app.tick.common.response.ApiResponse
import app.tick.news.application.CollectNewsForSymbolUseCase
import app.tick.news.application.CollectResult
import app.tick.news.application.GetRecentNewsUseCase
import app.tick.news.application.NewsResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/news")
class NewsController(
    private val getRecentNews: GetRecentNewsUseCase,
    private val collectNews: CollectNewsForSymbolUseCase,
) {
    @GetMapping("/{symbol}")
    fun recent(
        @PathVariable symbol: String,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ApiResponse<List<NewsResult>> =
        ApiResponse.success(getRecentNews.recent(StockCode(symbol), limit.coerceIn(1, 50)))

    // 수동 수집 트리거. Phase 4 cron 도입 전까지는 admin/manual 용.
    @PostMapping("/{symbol}/collect")
    fun collect(
        @PathVariable symbol: String,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ApiResponse<CollectResult> =
        ApiResponse.success(collectNews.collect(StockCode(symbol), limit.coerceIn(1, 100)))
}
