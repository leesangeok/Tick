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
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import java.net.http.HttpClient
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

@ConfigurationProperties(prefix = "tick.stock-quote.kis")
data class KisStockQuoteProperties(
    /** 실전 URL. 모의(vts) 는 `https://openapivts.koreainvestment.com:29443`. */
    val baseUrl: String = "https://openapi.koreainvestment.com:9443",
    val appKey: String = "",
    val appSecret: String = "",
    /** 현재가 캐시 TTL (초). rate limit (실전 20 TPS) 방어 겸용. */
    val quoteTtlSec: Long = 30,
    /** 일봉 시계열 캐시 TTL (초). 일봉은 거의 안 바뀌므로 길게. */
    val seriesTtlSec: Long = 3600,
    /**
     * access token 캐시 TTL (초). KIS 는 발급 시 24h 만료를 준다.
     * KIS 는 "1분당 재발급 1회" 제한이 있어 토큰 캐시 필수. 안전 마진 1시간.
     */
    val tokenTtlSec: Long = 60 * 60 * 23,
)

@Configuration
@EnableConfigurationProperties(KisStockQuoteProperties::class)
class KisStockQuoteConfig

/**
 * 한국투자증권 KIS Open API 어댑터. **시세 조회 전용** (읽기).
 *
 * - Base URL 은 실전(`openapi.koreainvestment.com:9443`) / 모의(`openapivts.koreainvestment.com:29443`)
 *   토글 가능. 이 어댑터는 어느 쪽이든 동일 endpoint 스펙 사용.
 * - Auth: POST `/oauth2/tokenP` (client_credentials) → access_token (24h TTL).
 *   Redis + in-memory 이중 캐시. Redis 다운 시에도 in-memory 로 정상 동작.
 * - 현재가: GET `/uapi/domestic-stock/v1/quotations/inquire-price` (`tr_id=FHKST01010100`)
 * - 일봉:   GET `/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice`
 *          (`tr_id=FHKST03010100`, 최근 N영업일 조회 후 tail(days))
 *
 * 캐싱:
 * - Redis 에 JSON 직렬화 + EXPIRE. key: `stock:kis:quote:{symbol}` (TTL 30s),
 *   `stock:kis:series:{symbol}:{days}` (TTL 1h), `stock:kis:token` (TTL ≒ 23h).
 * - Redis 다운 시 캐시 우회 → 매번 KIS 직접 호출 (graceful — fail-open).
 *
 * Fallback:
 * - KIS 실패 (token 실패 / API 응답 이상 / 네트워크 에러) 시 [StockPriceGenerator] mulberry32 로
 *   graceful fallback. 도메인 호출이 끊기지 않게.
 *
 * 활성화 토글: `tick.stock-quote.provider=kis`. 미설정 시 yahoo (기본).
 *
 * **주의**: 이 어댑터는 계좌 정보 / 주문 API 를 절대 호출하지 않는다. 시세 endpoint 만 사용.
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "tick.stock-quote", name = ["provider"], havingValue = "kis")
class KisStockQuoteAdapter(
    private val properties: KisStockQuoteProperties,
    private val stockMasterRepository: StockMasterRepository,
    private val stockPriceGenerator: StockPriceGenerator,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : StockQuoteProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client: RestClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()),
        )
        .defaultHeader("User-Agent", "Tick/1.0")
        .build()

    private val seoul: ZoneId = ZoneId.of("Asia/Seoul")
    private val kisDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val seriesTypeRef = object : TypeReference<List<StockPricePoint>>() {}

    // Redis 다운 대비 in-memory 토큰 캐시.
    private val memoryToken = AtomicReference<CachedToken?>(null)

    override fun quote(symbol: String): StockQuote? {
        val cacheKey = "$QUOTE_KEY_PREFIX$symbol"
        cacheGet(cacheKey, StockQuote::class.java)?.let { return it }

        val master = stockMasterRepository.findById(symbol).orElse(null) ?: return null

        return try {
            val token = accessToken() ?: return fallbackQuote(master, "KIS token unavailable")
            val response = client.get()
                .uri("/uapi/domestic-stock/v1/quotations/inquire-price?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD={s}", symbol)
                .header("authorization", "Bearer $token")
                .header("appkey", properties.appKey)
                .header("appsecret", properties.appSecret)
                .header("tr_id", "FHKST01010100")
                .header("custtype", "P")
                .retrieve()
                .body<KisPriceResponse>()
                ?: error("KIS returned empty body")

            val quote = response.toStockQuote(symbol)
                ?: return fallbackQuote(master, "KIS price rt_cd=${response.rtCd} msg=${response.msg1}")
            cachePut(cacheKey, quote, Duration.ofSeconds(properties.quoteTtlSec))
            quote
        } catch (e: RestClientException) {
            fallbackQuote(master, "KIS call failed: ${e.message}")
        } catch (e: IllegalStateException) {
            fallbackQuote(master, e.message ?: "KIS response error")
        }
    }

    override fun priceSeries(symbol: String, days: Int): List<StockPricePoint> {
        val cacheKey = "$SERIES_KEY_PREFIX$symbol:$days"
        cacheGetList(cacheKey, seriesTypeRef)?.let { return it }

        val master = stockMasterRepository.findById(symbol).orElse(null) ?: return emptyList()

        // 주말/공휴일 감안해서 넉넉하게 조회 후 tail(days). KIS 는 100영업일 제한이 있어
        // days 가 100 을 넘으면 페이지네이션 필요하지만 현 시나리오(30일 이하)는 여유.
        val end = LocalDate.now(seoul)
        val start = end.minusDays((days * 2L).coerceAtLeast(days + 14L))

        return try {
            val token = accessToken() ?: return fallbackSeries(master, days, "KIS token unavailable")
            val response = client.get()
                .uri(
                    "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice" +
                        "?FID_COND_MRKT_DIV_CODE=J" +
                        "&FID_INPUT_ISCD={s}" +
                        "&FID_INPUT_DATE_1={start}" +
                        "&FID_INPUT_DATE_2={end}" +
                        "&FID_PERIOD_DIV_CODE=D" +
                        "&FID_ORG_ADJ_PRC=0",
                    symbol, start.format(kisDateFmt), end.format(kisDateFmt),
                )
                .header("authorization", "Bearer $token")
                .header("appkey", properties.appKey)
                .header("appsecret", properties.appSecret)
                .header("tr_id", "FHKST03010100")
                .header("custtype", "P")
                .retrieve()
                .body<KisChartResponse>()
                ?: error("KIS returned empty body")

            val points = response.toPricePoints().takeLast(days)
            if (points.isEmpty()) {
                return fallbackSeries(master, days, "KIS chart empty rt_cd=${response.rtCd} msg=${response.msg1}")
            }
            cachePut(cacheKey, points, Duration.ofSeconds(properties.seriesTtlSec))
            points
        } catch (e: RestClientException) {
            fallbackSeries(master, days, "KIS call failed: ${e.message}")
        } catch (e: IllegalStateException) {
            fallbackSeries(master, days, e.message ?: "KIS response error")
        }
    }

    // ---------- token ----------

    private fun accessToken(): String? {
        cacheGet(TOKEN_KEY, CachedToken::class.java)?.let {
            if (it.expiresAtEpochMs > System.currentTimeMillis()) return it.token
        }
        memoryToken.get()?.let {
            if (it.expiresAtEpochMs > System.currentTimeMillis()) return it.token
        }
        return issueToken()
    }

    // 여러 스레드가 동시에 만료를 감지해도 발급은 1회만 나가도록 락.
    @Synchronized
    private fun issueToken(): String? {
        memoryToken.get()?.let {
            if (it.expiresAtEpochMs > System.currentTimeMillis()) return it.token
        }
        if (properties.appKey.isBlank() || properties.appSecret.isBlank()) {
            log.warn("KIS appKey/appSecret not configured")
            return null
        }
        return try {
            val body = mapOf(
                "grant_type" to "client_credentials",
                "appkey" to properties.appKey,
                "appsecret" to properties.appSecret,
            )
            val response = client.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body<KisTokenResponse>()
                ?: error("KIS token endpoint returned empty body")
            val token = response.accessToken ?: error("KIS token response missing access_token")
            val expiresInSec = (response.expiresIn ?: properties.tokenTtlSec).coerceAtMost(properties.tokenTtlSec)
            val cached = CachedToken(
                token = token,
                expiresAtEpochMs = System.currentTimeMillis() + expiresInSec * 1000L,
            )
            memoryToken.set(cached)
            // 안전 마진 1분: Redis TTL 을 토큰 만료보다 살짝 짧게.
            cachePut(TOKEN_KEY, cached, Duration.ofSeconds((expiresInSec - 60).coerceAtLeast(60L)))
            log.info("KIS token issued, expiresInSec={}", expiresInSec)
            token
        } catch (e: Exception) {
            log.warn("KIS token issue failed: {}", e.message)
            null
        }
    }

    // ---------- redis helpers (Yahoo adapter 와 동일 패턴) ----------

    private fun <T> cacheGet(key: String, type: Class<T>): T? = try {
        redisTemplate.opsForValue().get(key)?.let { objectMapper.readValue(it, type) }
    } catch (e: RedisConnectionFailureException) {
        log.debug("redis down on get key={} ({}). falling through.", key, e.message); null
    } catch (e: Exception) {
        log.warn("redis get failed key={} err={}. falling through.", key, e.message); null
    }

    private fun <T> cacheGetList(key: String, type: TypeReference<T>): T? = try {
        redisTemplate.opsForValue().get(key)?.let { objectMapper.readValue(it, type) }
    } catch (e: RedisConnectionFailureException) {
        log.debug("redis down on get key={} ({}). falling through.", key, e.message); null
    } catch (e: Exception) {
        log.warn("redis get failed key={} err={}. falling through.", key, e.message); null
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
        log.warn("kis quote fallback symbol={} reason={}", master.symbol, reason)
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
        log.warn("kis series fallback symbol={} days={} reason={}", master.symbol, days, reason)
        return stockPriceGenerator.generate(master.symbol, master.basePrice, days).map {
            StockPricePoint(it.date, it.open, it.high, it.low, it.close, it.volume)
        }
    }

    data class CachedToken(val token: String, val expiresAtEpochMs: Long)

    companion object {
        private const val QUOTE_KEY_PREFIX = "stock:kis:quote:"
        private const val SERIES_KEY_PREFIX = "stock:kis:series:"
        private const val TOKEN_KEY = "stock:kis:token"
    }

    // ---------- response DTOs ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KisTokenResponse(
        @JsonProperty("access_token") val accessToken: String?,
        @JsonProperty("token_type") val tokenType: String?,
        @JsonProperty("expires_in") val expiresIn: Long?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KisPriceResponse(
        @JsonProperty("rt_cd") val rtCd: String?,
        @JsonProperty("msg_cd") val msgCd: String?,
        val msg1: String?,
        val output: KisPriceOutput?,
    ) {
        fun toStockQuote(symbol: String): StockQuote? {
            val out = output ?: return null
            val current = out.stckPrpr?.toIntOrNull() ?: return null
            val previous = out.stckSdpr?.toIntOrNull() ?: return null
            val changeAmount = out.prdyVrss?.toIntOrNull() ?: (current - previous)
            val changeRate = out.prdyCtrt?.toDoubleOrNull() ?: 0.0
            val volume = out.acmlVol?.toLongOrNull() ?: 0L
            return StockQuote(symbol, current, previous, changeAmount, changeRate, volume)
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KisPriceOutput(
        @JsonProperty("stck_prpr") val stckPrpr: String?,   // 주식 현재가
        @JsonProperty("stck_sdpr") val stckSdpr: String?,   // 전일 종가
        @JsonProperty("prdy_vrss") val prdyVrss: String?,   // 전일 대비
        @JsonProperty("prdy_ctrt") val prdyCtrt: String?,   // 전일 대비율 (%)
        @JsonProperty("acml_vol") val acmlVol: String?,     // 누적 거래량
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KisChartResponse(
        @JsonProperty("rt_cd") val rtCd: String?,
        @JsonProperty("msg_cd") val msgCd: String?,
        val msg1: String?,
        val output2: List<KisChartPoint>?,
    ) {
        fun toPricePoints(): List<StockPricePoint> {
            val list = output2 ?: return emptyList()
            val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
            return list.mapNotNull { p ->
                val date = p.stckBsopDate?.let { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }
                    ?: return@mapNotNull null
                val open = p.stckOprc?.toIntOrNull() ?: return@mapNotNull null
                val high = p.stckHgpr?.toIntOrNull() ?: return@mapNotNull null
                val low = p.stckLwpr?.toIntOrNull() ?: return@mapNotNull null
                val close = p.stckClpr?.toIntOrNull() ?: return@mapNotNull null
                val volume = p.acmlVol?.toLongOrNull() ?: 0L
                StockPricePoint(date, open, high, low, close, volume)
            }.sortedBy { it.date }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class KisChartPoint(
        @JsonProperty("stck_bsop_date") val stckBsopDate: String?,  // 영업일자 yyyyMMdd
        @JsonProperty("stck_clpr") val stckClpr: String?,           // 종가
        @JsonProperty("stck_oprc") val stckOprc: String?,           // 시가
        @JsonProperty("stck_hgpr") val stckHgpr: String?,           // 고가
        @JsonProperty("stck_lwpr") val stckLwpr: String?,           // 저가
        @JsonProperty("acml_vol") val acmlVol: String?,             // 누적 거래량
    )
}
