package app.tick.news.adapter.external

import app.tick.common.domain.StockCode
import app.tick.news.application.NewsCollectorPort
import app.tick.news.domain.News
import app.tick.stock.StockMasterRepository
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Configuration
@EnableConfigurationProperties(NaverNewsProperties::class)
class NaverNewsConfig

@Component
class NaverNewsAdapter(
    private val properties: NaverNewsProperties,
    private val stockMasterRepository: StockMasterRepository,
) : NewsCollectorPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client: RestClient = RestClient.create()

    // RFC 1123 (네이버 pubDate 형식: "Mon, 16 Jun 2026 12:34:56 +0900")
    private val pubDateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)

    override fun search(stockCode: StockCode, limit: Int): List<News> {
        if (properties.clientId.isBlank() || properties.clientSecret.isBlank()) {
            log.warn("Naver client id/secret not configured. Skipping news fetch for {}.", stockCode.value)
            return emptyList()
        }

        val stockName = stockMasterRepository.findById(stockCode.value).orElse(null)?.name
            ?: run {
                log.warn("Stock master not found for {}. Skipping news fetch.", stockCode.value)
                return emptyList()
            }

        val response = client.get()
            .uri("${properties.baseUrl}?query={q}&display={d}&sort=date", stockName, limit.coerceIn(1, 100))
            .header("X-Naver-Client-Id", properties.clientId)
            .header("X-Naver-Client-Secret", properties.clientSecret)
            .retrieve()
            .body<NaverNewsResponse>()

        return response?.items.orEmpty().mapNotNull { item ->
            try {
                News.newOne(
                    stockCode = stockCode,
                    title = stripHtml(item.title),
                    body = stripHtml(item.description),
                    source = item.originalLink?.let { extractDomain(it) },
                    sourceUrl = item.link ?: item.originalLink,
                    publishedAt = parsePubDate(item.pubDate),
                )
            } catch (e: Exception) {
                log.warn("Skipping malformed naver news item: {}", e.message)
                null
            }
        }
    }

    private fun stripHtml(s: String): String =
        s.replace(Regex("<[^>]+>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&apos;", "'")
            .trim()

    private fun extractDomain(url: String): String? =
        Regex("""^https?://([^/]+)""").find(url)?.groupValues?.getOrNull(1)

    private fun parsePubDate(s: String): Instant =
        ZonedDateTime.parse(s, pubDateFormatter).toInstant()
}

data class NaverNewsResponse(
    val items: List<NaverNewsItem> = emptyList(),
)

data class NaverNewsItem(
    val title: String,
    val description: String,
    @JsonProperty("originallink") val originalLink: String?,
    val link: String?,
    @JsonProperty("pubDate") val pubDate: String,
)
