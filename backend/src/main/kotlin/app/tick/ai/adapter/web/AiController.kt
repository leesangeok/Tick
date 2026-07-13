package app.tick.ai.adapter.web

import app.tick.ai.application.AiSummaryResult
import app.tick.ai.application.EmbedResult
import app.tick.ai.application.EmbedStockNewsUseCase
import app.tick.ai.application.GetAiSummaryUseCase
import app.tick.common.domain.StockCode
import app.tick.common.ratelimit.RateLimited
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
    // AI 요약은 LLM 호출 (원가 큼) 이라 rate limit 을 별도 bucket 으로 좁게 잡음.
    @RateLimited(bucket = "ai_summary", limit = 5, windowSec = 60)
    @GetMapping("/{symbol}/summary")
    fun summary(@PathVariable symbol: String): ApiResponse<AiSummaryResult> =
        ApiResponse.success(getSummary.summary(StockCode(symbol)))

    // Phase 4 cron 도입 전까지 임베딩 수동 트리거 (admin/manual).
    // 스케줄러 도입 이후에도 최소 사용은 유지 — 트리거 남용 방지 위해 좁은 quota.
    @RateLimited(bucket = "ai_embed", limit = 3, windowSec = 60)
    @PostMapping("/{symbol}/embed")
    fun embed(@PathVariable symbol: String): ApiResponse<EmbedResult> =
        ApiResponse.success(embedNews.embed(StockCode(symbol)))
}
