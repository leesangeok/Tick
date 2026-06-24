package app.tick.stock.adapter

import app.tick.stock.StockMaster
import app.tick.stock.StockMasterRepository
import app.tick.stock.StockPriceGenerator
import app.tick.stock.StockPricePoint
import app.tick.stock.StockQuote
import app.tick.stock.StockQuoteProvider
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import java.net.http.HttpClient
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@ConfigurationProperties(prefix = "tick.stock-quote.yahoo")
data class YahooStockQuoteProperties(
    val baseUrl: String = "https://query1.finance.yahoo.com",
    /** 현재가 캐시 TTL (초). 모의투자라 30초면 충분. Yahoo rate limit 방어 겸용. */
    val quoteTtlSec: Long = 30,
    /** 일봉 시계열 캐시 TTL (초). 일봉은 거의 안 바뀌므로 길게. */
    val seriesTtlSec: Long = 3600,
)

@Configuration
@EnableConfigurationProperties(YahooStockQuoteProperties::class)
class YahooStockQuoteConfig

/**
 * Yahoo Finance chart API 어댑터.
 *
 * - endpoint: GET {baseUrl}/v8/finance/chart/{ticker}?interval=1d&range={days}d
 * - 한국 종목 ticker: backend symbol("005930") + suffix("KOSPI" → ".KS", "KOSDAQ" → ".KQ")
 * - 응답: chart.result[0].meta.regularMarketPrice / chartPreviousClose / regularMarketVolume
 *         + chart.result[0].timestamp[] + chart.result[0].indicators.quote[0].{open,high,low,close,volume}[]
 * - 현재가는 15분 지연. 모의투자 학습용엔 충분.
 *
 * 캐싱:
 * - Redis 에 JSON 직렬화 + EXPIRE TTL. 다중 backend 인스턴스 공유, 재시작에도 보존.
 * - key: `stock:quote:{symbol}` (TTL 30s), `stock:series:{symbol}:{days}` (TTL 1h)
 * - Redis 다운 시에는 캐시 우회 후 매번 Yahoo 직접 호출 (graceful — fail-open).
 *
 * Fallback:
 * - Yahoo 실패 시 [StockPriceGenerator] (mulberry32) 로 graceful fallback. 도메인 호출이 끊기지 않게.
 *
 * 활성화 토글: `tick.stock-quote.provider=yahoo` (기본 yahoo). `mulberry` 면 본 bean 비활성 →
 * [MulberryStockQuoteAdapter] 가 등록됨.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "tick.stock-quote", name = ["provider"], havingValue = "yahoo", matchIfMissing = true)
class YahooStockQuoteAdapter(
    private val properties: YahooStockQuoteProperties,
    private val stockMasterRepository: StockMasterRepository,
    private val stockPriceGenerator: StockPriceGenerator,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : StockQuoteProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    // Yahoo 가 default Java HttpClient UA 를 가끔 차단하므로 명시적 UA 설정.
    private val client: RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()),
        )
        .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; Tick/1.0; +https://tickk.dev)")
        .build()

    private val seoul: ZoneId = ZoneId.of("Asia/Seoul")
    private val seriesTypeRef = object : TypeReference<List<StockPricePoint>>() {}

    override fun quote(symbol: String): StockQuote? {
        val cacheKey = "$QUOTE_KEY_PREFIX$symbol"
        cacheGet(cacheKey, StockQuote::class.java)?.let { return it }

        val master = stockMasterRepository.findById(symbol).orElse(null) ?: return null
        val ticker = toYahooTicker(master)

        return try {
            val response = client.get()
                .uri("/v8/finance/chart/{ticker}?interval=1d&range=2d", ticker)
                .retrieve()
                .body<YahooChartResponse>()
                ?: error("Yahoo returned empty body")
            val quote = response.toStockQuote(symbol)
                ?: return fallbackQuote(master, "Yahoo response missing required fields")
            cachePut(cacheKey, quote, Duration.ofSeconds(properties.quoteTtlSec))
            quote
        } catch (e: RestClientException) {
            fallbackQuote(master, "Yahoo call failed: ${e.message}")
        } catch (e: IllegalStateException) {
            fallbackQuote(master, e.message ?: "Yahoo response error")
        }
    }

    override fun priceSeries(symbol: String, days: Int): List<StockPricePoint> {
        val cacheKey = "$SERIES_KEY_PREFIX$symbol:$days"
        cacheGetList(cacheKey, seriesTypeRef)?.let { return it }

        val master = stockMasterRepository.findById(symbol).orElse(null) ?: return emptyList()
        val ticker = toYahooTicker(master)

        return try {
            val response = client.get()
                .uri("/v8/finance/chart/{ticker}?interval=1d&range={days}d", ticker, days)
                .retrieve()
                .body<YahooChartResponse>()
                ?: error("Yahoo returned empty body")
            val points = response.toPricePoints(seoul)
            if (points.isEmpty()) return fallbackSeries(master, days, "Yahoo returned no chart points")
            cachePut(cacheKey, points, Duration.ofSeconds(properties.seriesTtlSec))
            points
        } catch (e: RestClientException) {
            fallbackSeries(master, days, "Yahoo call failed: ${e.message}")
        } catch (e: IllegalStateException) {
            fallbackSeries(master, days, e.message ?: "Yahoo response error")
        }
    }

    private fun <T> cacheGet(key: String, type: Class<T>): T? = try {
        redisTemplate.opsForValue().get(key)?.let { objectMapper.readValue(it, type) }
    } catch (e: RedisConnectionFailureException) {
        log.debug("redis down on get key={} ({}). falling through to Yahoo.", key, e.message)
        null
    } catch (e: Exception) {
        log.warn("redis get failed key={} err={}. falling through.", key, e.message)
        null
    }

    private fun <T> cacheGetList(key: String, type: TypeReference<T>): T? = try {
        redisTemplate.opsForValue().get(key)?.let { objectMapper.readValue(it, type) }
    } catch (e: RedisConnectionFailureException) {
        log.debug("redis down on get key={} ({}). falling through to Yahoo.", key, e.message)
        null
    } catch (e: Exception) {
        log.warn("redis get failed key={} err={}. falling through.", key, e.message)
        null
    }

    private fun cachePut(key: String, value: Any, ttl: Duration) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl)
        } catch (e: RedisConnectionFailureException) {
            log.debug("redis down on set key={} ({}). skipping cache.", key, e.message)
        } catch (e: Exception) {
            log.warn("redis set failed key={} err={}", key, e.message)
        }
    }

    private fun fallbackQuote(master: StockMaster, reason: String): StockQuote {
        log.warn("yahoo fallback symbol={} reason={}", master.symbol, reason)
        val series = stockPriceGenerator.generate(master.symbol, master.basePrice, 2)
        val last = series.last()
        val prev = series.first()
        val changeAmount = last.close - prev.close
        val changeRate = if (prev.close != 0) changeAmount.toDouble() / prev.close * 100.0 else 0.0
        return StockQuote(
            symbol = master.symbol,
            currentPrice = last.close,
            previousClose = prev.close,
            changeAmount = changeAmount,
            changeRate = changeRate,
            volume = last.volume,
        )
    }

    private fun fallbackSeries(master: StockMaster, days: Int, reason: String): List<StockPricePoint> {
        log.warn("yahoo series fallback symbol={} days={} reason={}", master.symbol, days, reason)
        return stockPriceGenerator.generate(master.symbol, master.basePrice, days).map {
            StockPricePoint(it.date, it.open, it.high, it.low, it.close, it.volume)
        }
    }

    private fun toYahooTicker(master: StockMaster): String = when (master.market.uppercase()) {
        "KOSPI" -> "${master.symbol}.KS"
        "KOSDAQ" -> "${master.symbol}.KQ"
        else -> "${master.symbol}.KS"
    }

    companion object {
        private const val QUOTE_KEY_PREFIX = "stock:quote:"
        private const val SERIES_KEY_PREFIX = "stock:series:"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class YahooChartResponse(val chart: YahooChart) {
        fun toStockQuote(symbol: String): StockQuote? {
            val result = chart.result?.firstOrNull() ?: return null
            val meta = result.meta ?: return null
            val price = meta.regularMarketPrice ?: return null
            val previous = meta.chartPreviousClose ?: return null
            val changeAmount = (price - previous).roundToInt()
            val changeRate = if (previous != 0.0) (price - previous) / previous * 100.0 else 0.0
            return StockQuote(
                symbol = symbol,
                currentPrice = price.roundToInt(),
                previousClose = previous.roundToInt(),
                changeAmount = changeAmount,
                changeRate = changeRate,
                volume = meta.regularMarketVolume ?: 0L,
            )
        }

        fun toPricePoints(zone: ZoneId): List<StockPricePoint> {
            val result = chart.result?.firstOrNull() ?: return emptyList()
            val timestamps = result.timestamp ?: return emptyList()
            val quote = result.indicators?.quote?.firstOrNull() ?: return emptyList()
            val opens = quote.open ?: return emptyList()
            val highs = quote.high ?: return emptyList()
            val lows = quote.low ?: return emptyList()
            val closes = quote.close ?: return emptyList()
            val volumes = quote.volume ?: return emptyList()
            return timestamps.mapIndexedNotNull { i, ts ->
                val o = opens.getOrNull(i)
                val h = highs.getOrNull(i)
                val l = lows.getOrNull(i)
                val c = closes.getOrNull(i)
                val v = volumes.getOrNull(i)
                if (o == null || h == null || l == null || c == null) null
                else StockPricePoint(
                    date = Instant.ofEpochSecond(ts).atZone(zone).toLocalDate(),
                    open = o.roundToInt(),
                    high = h.roundToInt(),
                    low = l.roundToInt(),
                    close = c.roundToInt(),
                    volume = v ?: 0L,
                )
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class YahooChart(val result: List<YahooResult>?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class YahooResult(
        val meta: YahooMeta?,
        val timestamp: List<Long>?,
        val indicators: YahooIndicators?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class YahooMeta(
        @JsonProperty("regularMarketPrice") val regularMarketPrice: Double?,
        @JsonProperty("chartPreviousClose") val chartPreviousClose: Double?,
        @JsonProperty("regularMarketVolume") val regularMarketVolume: Long?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class YahooIndicators(val quote: List<YahooQuote>?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class YahooQuote(
        val open: List<Double?>?,
        val high: List<Double?>?,
        val low: List<Double?>?,
        val close: List<Double?>?,
        val volume: List<Long?>?,
    )
}
