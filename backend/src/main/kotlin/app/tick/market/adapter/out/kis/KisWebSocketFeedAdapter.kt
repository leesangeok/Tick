package app.tick.market.adapter.out.kis

import app.tick.market.application.MarketProperties
import app.tick.market.application.port.out.BroadcastPort
import app.tick.market.application.port.out.MarketFeedPort
import app.tick.market.domain.PriceTick
import app.tick.stock.adapter.KisStockQuoteProperties
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

/**
 * KIS 실시간 시세 WebSocket 어댑터.
 *
 * 흐름:
 * 1. `POST /oauth2/Approval` 로 `approval_key` 발급 (WS 전용, REST access_token 과 별개)
 * 2. `ws://ops.koreainvestment.com:21000` 접속
 * 3. `MarketService` 가 [subscribe]/[unsubscribe] 호출 → JSON subscribe 메시지 전송
 * 4. KIS 응답 처리:
 *    - `{"header":{"tr_id":"PINGPONG"...}}` → 그대로 echo back (heartbeat)
 *    - `{"header":{"tr_id":"H0STCNT0"...,"body":{"msg1":"SUBSCRIBE SUCCESS"...}}` → 구독 확인 로그
 *    - `0|H0STCNT0|N|005930^123456^71000^5^1500^2.07^...` → pipe 파싱 → PriceTick → BroadcastPort
 * 5. 연결 끊기면 지수 backoff 재접속 + 활성 심볼 재구독
 *
 * `tick.market.enabled=false` 면 bean 미등록. `MarketService` 는 `@ConditionalOnBean` 이 아니라
 * 언제나 필요하므로 (설정 조회 등) 이 어댑터가 없으면 no-op `DisabledMarketFeedAdapter` 로 대체 —
 * 아래 [KisWebSocketFeedAdapterFallback] 참고.
 *
 * 스레드 모델:
 * - `WebSocket.Listener` 콜백은 JDK HttpClient executor 에서 실행 (분리된 스레드 풀 지정)
 * - `send()` 는 직렬화 필수 — `sendLock` 으로 보호
 * - 재연결은 `scheduler` (single thread) 에서 스케줄
 */
@Component
@ConditionalOnProperty(prefix = "tick.market", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class KisWebSocketFeedAdapter(
    private val kisProperties: KisStockQuoteProperties,
    private val marketProperties: MarketProperties,
    private val broadcastPort: BroadcastPort,
    private val objectMapper: ObjectMapper,
) : MarketFeedPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val seoul: ZoneId = ZoneId.of("Asia/Seoul")

    /** JDK WebSocket / 재연결 스케줄 전용 executor. daemon thread 로 앱 종료 안 막게. */
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "kis-ws-scheduler").apply { isDaemon = true }
    }
    private val wsExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "kis-ws-io").apply { isDaemon = true }
    }
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .executor(wsExecutor)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    /** 활성 심볼. 재연결 시 여기 있는 심볼 전부 다시 구독. */
    private val activeSymbols: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val currentWs = AtomicReference<WebSocket?>(null)
    private val approvalKey = AtomicReference<String?>(null)
    private val sendLock = Any()

    // 프래그먼트 재조합용 (JDK WS 는 텍스트 프레임이 나뉘어 올 수 있음).
    private val textBuffer = StringBuilder()

    @Volatile
    private var closed = false

    @Volatile
    private var reconnectAttempts = 0

    @PreDestroy
    fun shutdown() {
        closed = true
        currentWs.get()?.let { runCatching { it.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown").get(1, TimeUnit.SECONDS) } }
        scheduler.shutdownNow()
        wsExecutor.shutdownNow()
    }

    @PostConstruct
    fun init() {
        log.info("KisWebSocketFeedAdapter registered (lazy connect on first subscribe)")
    }

    override fun subscribe(symbol: String) {
        if (!activeSymbols.add(symbol)) return  // 이미 구독 중
        ensureConnected()
        sendSubscribeMessage(symbol, register = true)
    }

    override fun unsubscribe(symbol: String) {
        if (!activeSymbols.remove(symbol)) return
        // 이미 연결이 끊긴 상태면 굳이 재접속해서 unsubscribe 보낼 필요 없음.
        val ws = currentWs.get() ?: return
        if (!ws.isInputClosed && !ws.isOutputClosed) sendSubscribeMessage(symbol, register = false)
    }

    // ---------- connection ----------

    @Synchronized
    private fun ensureConnected() {
        val ws = currentWs.get()
        if (ws != null && !ws.isInputClosed && !ws.isOutputClosed) return
        connect()
    }

    private fun connect() {
        if (closed) return
        try {
            val key = obtainApprovalKey() ?: run {
                log.warn("approval_key unavailable, scheduling retry")
                scheduleReconnect()
                return
            }
            log.info("KIS WS connecting url={}", marketProperties.wsUrl)
            HttpClient.newBuilder()
                .executor(wsExecutor)
                .build()
                .newWebSocketBuilder()
                .buildAsync(URI.create(marketProperties.wsUrl), KisListener(key))
                .whenComplete { ws, err ->
                    if (err != null) {
                        log.warn("KIS WS connect failed: {}", err.message)
                        scheduleReconnect()
                    } else {
                        currentWs.set(ws)
                        reconnectAttempts = 0
                        log.info("KIS WS connected, resubscribing {} symbols", activeSymbols.size)
                        activeSymbols.forEach { sendSubscribeMessage(it, register = true) }
                    }
                }
        } catch (e: Exception) {
            log.warn("KIS WS connect exception: {}", e.message)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (closed) return
        reconnectAttempts += 1
        val backoff = min(marketProperties.reconnectMaxBackoffSec, (1L shl min(reconnectAttempts - 1, 6)))
        log.info("KIS WS reconnect scheduled in {}s (attempt={})", backoff, reconnectAttempts)
        scheduler.schedule({ connect() }, backoff, TimeUnit.SECONDS)
    }

    private fun obtainApprovalKey(): String? {
        approvalKey.get()?.let { return it }
        if (kisProperties.appKey.isBlank() || kisProperties.appSecret.isBlank()) {
            log.warn("KIS appKey/secret missing — cannot obtain approval_key")
            return null
        }
        return try {
            val body = objectMapper.writeValueAsString(
                mapOf(
                    "grant_type" to "client_credentials",
                    "appkey" to kisProperties.appKey,
                    "secretkey" to kisProperties.appSecret,
                ),
            )
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${kisProperties.baseUrl}/oauth2/Approval"))
                .header("content-type", "application/json; utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                log.warn("approval_key HTTP {}: {}", response.statusCode(), response.body().take(200))
                return null
            }
            val parsed = objectMapper.readTree(response.body())
            val key = parsed.get("approval_key")?.asText()
            if (key.isNullOrBlank()) {
                log.warn("approval_key missing in response: {}", response.body().take(200))
                null
            } else {
                approvalKey.set(key)
                log.info("approval_key issued")
                key
            }
        } catch (e: Exception) {
            log.warn("approval_key request failed: {}", e.message)
            null
        }
    }

    // ---------- send ----------

    private fun sendSubscribeMessage(symbol: String, register: Boolean) {
        val ws = currentWs.get() ?: return
        val key = approvalKey.get() ?: return
        val msg = objectMapper.writeValueAsString(
            mapOf(
                "header" to mapOf(
                    "approval_key" to key,
                    "custtype" to "P",
                    "tr_type" to if (register) "1" else "2",
                    "content-type" to "utf-8",
                ),
                "body" to mapOf(
                    "input" to mapOf(
                        "tr_id" to "H0STCNT0",
                        "tr_key" to symbol,
                    ),
                ),
            ),
        )
        sendRaw(ws, msg)
    }

    /** send() 는 concurrent 하게 부르면 IllegalStateException — 직렬화. */
    private fun sendRaw(ws: WebSocket, text: String) {
        synchronized(sendLock) {
            try {
                ws.sendText(text, true).get(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                log.warn("KIS WS send failed: {}", e.message)
            }
        }
    }

    // ---------- listener ----------

    private inner class KisListener(private val key: String) : WebSocket.Listener {
        override fun onOpen(webSocket: WebSocket) {
            log.info("KIS WS onOpen")
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            textBuffer.append(data)
            if (last) {
                val full = textBuffer.toString()
                textBuffer.setLength(0)
                try {
                    handleMessage(webSocket, full)
                } catch (e: Exception) {
                    log.warn("KIS WS message handle error: {}", e.message)
                }
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String?): CompletionStage<*>? {
            log.info("KIS WS onClose code={} reason={}", statusCode, reason)
            currentWs.compareAndSet(webSocket, null)
            scheduleReconnect()
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            log.warn("KIS WS onError: {}", error.message)
            currentWs.compareAndSet(webSocket, null)
            scheduleReconnect()
        }
    }

    private fun handleMessage(ws: WebSocket, raw: String) {
        if (raw.startsWith("{")) {
            // JSON: PINGPONG 또는 subscribe 응답
            val node = objectMapper.readTree(raw)
            val trId = node.path("header").path("tr_id").asText(null)
            if (trId == "PINGPONG") {
                // 그대로 echo — KIS 가 세션 유지 확인
                sendRaw(ws, raw)
                return
            }
            val msg1 = node.path("body").path("msg1").asText(null)
            if (msg1 != null) log.info("KIS WS response tr_id={} msg={}", trId, msg1)
            return
        }
        // 실시간 tick — pipe 구분
        parseTick(raw)?.let(broadcastPort::broadcast)
    }

    // ---------- parsing ----------

    /**
     * KIS `H0STCNT0` 실시간 체결. 형식: `{type}|{tr_id}|{count}|{body^ ...}`
     *
     * body 필드 순서 (KIS 실전 문서 기준):
     * 0=종목코드, 1=체결시간 HHMMSS, 2=현재가, 3=전일대비부호 (1상한 2상승 3보합 4하한 5하락),
     * 4=전일대비(양수), 5=전일대비율, 6=가중평균가, 7=시가, 8=고가, 9=저가,
     * 10=매도호가1, 11=매수호가1, 12=체결거래량, 13=누적거래량, ...
     */
    internal fun parseTick(raw: String): PriceTick? {
        val head = raw.split('|', limit = 4)
        if (head.size < 4) return null
        if (head[1] != "H0STCNT0") return null
        val count = head[2].toIntOrNull() ?: 1
        val body = head[3]
        val fields = body.split('^')
        val fieldsPerRecord = if (count > 0) fields.size / count else fields.size
        if (fieldsPerRecord < 14) return null
        // 여러 tick 이 하나 메시지로 올 때 마지막 것만 사용 (프론트가 원하는 건 최신).
        val offset = (count - 1) * fieldsPerRecord
        return try {
            val symbol = fields[offset + 0]
            val hhmmss = fields[offset + 1]
            val price = fields[offset + 2].toIntOrNull() ?: return null
            val sign = fields[offset + 3]
            val absDelta = fields[offset + 4].toIntOrNull() ?: 0
            val changeRate = fields[offset + 5].toDoubleOrNull() ?: 0.0
            val acmlVol = fields[offset + 13].toLongOrNull() ?: 0L
            val changeAmount = when (sign) {
                "4", "5" -> -absDelta   // 하한, 하락
                else -> absDelta
            }
            val signedRate = if (changeAmount < 0 && changeRate > 0) -changeRate else changeRate
            PriceTick(
                symbol = symbol,
                price = price,
                changeAmount = changeAmount,
                changeRate = signedRate,
                volume = acmlVol,
                at = toInstantKst(hhmmss),
            )
        } catch (e: Exception) {
            log.debug("tick parse failed: {}", e.message)
            null
        }
    }

    private fun toInstantKst(hhmmss: String): Instant {
        val today = LocalDate.now(seoul)
        val time = try {
            LocalTime.of(
                hhmmss.substring(0, 2).toInt(),
                hhmmss.substring(2, 4).toInt(),
                hhmmss.substring(4, 6).toInt(),
            )
        } catch (_: Exception) {
            LocalTime.now(seoul)
        }
        return ZonedDateTime.of(today, time, seoul).toInstant()
    }
}

// -----------------------------------------------------------------------------
// Fallback: tick.market.enabled=false 일 때 MarketService 가 의존할 no-op 어댑터.
// -----------------------------------------------------------------------------
@Component
@ConditionalOnProperty(prefix = "tick.market", name = ["enabled"], havingValue = "false")
class KisWebSocketFeedAdapterFallback : MarketFeedPort {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun subscribe(symbol: String) {
        log.debug("market disabled, ignoring subscribe symbol={}", symbol)
    }
    override fun unsubscribe(symbol: String) {}
}
