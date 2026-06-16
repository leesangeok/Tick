package app.tick.ai.adapter.external

import app.tick.ai.application.AiServerPort
import app.tick.ai.application.AiSummaryResult
import app.tick.ai.application.EmbedResult
import app.tick.ai.application.Evidence
import app.tick.common.domain.StockCode
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Duration

@ConfigurationProperties(prefix = "tick.ai-server")
data class AiServerProperties(
    val url: String = "http://ai-server:8000",
    val timeoutSec: Long = 30,
)

@Configuration
@EnableConfigurationProperties(AiServerProperties::class)
class AiServerConfig

@Component
class AiServerAdapter(
    private val properties: AiServerProperties,
) : AiServerPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client: RestClient = RestClient.builder()
        .baseUrl(properties.url)
        .build()

    override fun summarize(stockCode: StockCode, stockName: String): AiSummaryResult {
        val req = SummaryRequest(symbol = stockCode.value, stock_name = stockName)
        val res = client.post()
            .uri("/ai/summary")
            .contentType(MediaType.APPLICATION_JSON)
            .body(req)
            .retrieve()
            .body<SummaryResponse>()
            ?: error("ai-server returned empty body")
        return AiSummaryResult(
            summary = res.summary,
            evidences = res.evidences.map {
                Evidence(
                    title = it.title,
                    source = it.source,
                    sourceUrl = it.source_url,
                    publishedAt = it.published_at,
                )
            },
        )
    }

    override fun embed(stockCode: StockCode): EmbedResult {
        val res = client.post()
            .uri("/ai/embeddings/{symbol}", stockCode.value)
            .retrieve()
            .body<EmbedResponse>()
            ?: error("ai-server returned empty body")
        return EmbedResult(upserted = res.upserted)
    }

    @Suppress("PropertyName")
    private data class SummaryRequest(val symbol: String, val stock_name: String)

    @Suppress("PropertyName")
    private data class SummaryResponse(
        val summary: String,
        val evidences: List<EvidenceDto>,
    )

    @Suppress("PropertyName")
    private data class EvidenceDto(
        val title: String,
        val source: String?,
        @JsonProperty("source_url") val source_url: String?,
        @JsonProperty("published_at") val published_at: String,
    )

    private data class EmbedResponse(val upserted: Int)
}
