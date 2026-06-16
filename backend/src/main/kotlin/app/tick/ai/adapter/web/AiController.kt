package app.tick.ai.adapter.web

import app.tick.ai.application.AiSummaryResult
import app.tick.ai.application.EmbedResult
import app.tick.ai.application.EmbedStockNewsUseCase
import app.tick.ai.application.GetAiSummaryUseCase
import app.tick.common.domain.StockCode
import app.tick.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ai/stocks")
class AiController(
    private val getSummary: GetAiSummaryUseCase,
    private val embedNews: EmbedStockNewsUseCase,
) {
    @GetMapping("/{symbol}/summary")
    fun summary(@PathVariable symbol: String): ApiResponse<AiSummaryResult> =
        ApiResponse.success(getSummary.summary(StockCode(symbol)))

    // Phase 4 cron 도입 전까지 임베딩 수동 트리거 (admin/manual).
    @PostMapping("/{symbol}/embed")
    fun embed(@PathVariable symbol: String): ApiResponse<EmbedResult> =
        ApiResponse.success(embedNews.embed(StockCode(symbol)))
}
