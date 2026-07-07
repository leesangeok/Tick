package app.tick.ai.adapter.external

import app.tick.ai.application.AiServerPort
import app.tick.ai.application.AiSummaryResult
import app.tick.ai.application.EmbedResult
import app.tick.ai.application.KeyReason
import app.tick.ai.application.SummarySource
import app.tick.common.domain.StockCode
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.http.HttpClient
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
        .requestFactory(
            // JDK HttpClient 기본값(HTTP/2) → h2c upgrade 시도하면 uvicorn 이 chunked body 를 잃음.
            // HTTP/1.1 로 고정해 ai-server 가 안정적으로 body 를 받게 한다.
            JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build())
        )
        .build()

    override fun summarize(stockCode: StockCode, stockName: String): AiSummaryResult {
        val res = client.post()
            .uri("/ai/summary")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("symbol" to stockCode.value, "stock_name" to stockName))
            .retrieve()
            .body<SummaryResponse>()
            ?: error("ai-server returned empty body")
        return AiSummaryResult(
            symbol = res.symbol,
            summary = res.summary,
            keyReasons = res.keyReasons.map { KeyReason(text = it.text, sourceIndices = it.sourceIndices) },
            riskNotes = res.riskNotes,
            sources = res.sources.map {
                SummarySource(
                    newsId = it.newsId,
                    title = it.title,
                    source = it.source,
                    sourceUrl = it.sourceUrl,
                    publishedAt = it.publishedAt,
                )
            },
            retrievedCount = res.retrievedCount,
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

    private data class SummaryResponse(
        val symbol: String,
        val summary: String,
        @JsonProperty("key_reasons") val keyReasons: List<KeyReasonDto> = emptyList(),
        @JsonProperty("risk_notes") val riskNotes: List<String> = emptyList(),
        val sources: List<SourceDto> = emptyList(),
        @JsonProperty("retrieved_count") val retrievedCount: Int,
    )

    private data class KeyReasonDto(
        val text: String,
        @JsonProperty("source_indices") val sourceIndices: List<Int> = emptyList(),
    )

    private data class SourceDto(
        @JsonProperty("news_id") val newsId: Long,
        val title: String,
        val source: String?,
        @JsonProperty("source_url") val sourceUrl: String?,
        @JsonProperty("published_at") val publishedAt: String,
    )

    private data class EmbedResponse(val upserted: Int)
}
