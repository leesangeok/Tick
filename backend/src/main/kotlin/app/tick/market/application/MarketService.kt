package app.tick.market.application

import app.tick.market.application.port.`in`.SubscribeMarketUseCase
import app.tick.market.application.port.`in`.SubscriptionCapExceededException
import app.tick.market.application.port.`in`.UnsubscribeMarketUseCase
import app.tick.market.application.port.out.MarketFeedPort
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@ConfigurationProperties(prefix = "tick.market.kis")
data class MarketProperties(
    val wsUrl: String = "ws://ops.koreainvestment.com:21000",
    /** KIS 세션당 tr_key 최대 개수. 실전/모의 공통 40. */
    val subscriptionCap: Int = 40,
    val pingpongTimeoutSec: Long = 30,
    val reconnectMaxBackoffSec: Long = 60,
)

/**
 * 프론트 세션 ↔ 종목 구독 매핑을 관리하고 upstream 어댑터를 호출.
 *
 * - `sessionToSymbols`: 세션 종료 시 그 세션이 잡고 있던 모든 심볼 정리를 O(1) 로.
 * - `symbolRefs`: 심볼별 참조 카운트. 0→1 전이에서 upstream subscribe, 1→0 에서 upstream unsubscribe.
 *
 * 동시성: WebSocket handler 는 여러 스레드에서 호출 가능. synchronized(this) 로 상태 전이 원자성 확보.
 * hot path 는 tick broadcast 인데 그건 BroadcastAdapter 쪽이라 이 서비스와 무관.
 */
@Service
class MarketService(
    private val marketFeedPort: MarketFeedPort,
    private val properties: MarketProperties,
) : SubscribeMarketUseCase, UnsubscribeMarketUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    private val sessionToSymbols = ConcurrentHashMap<String, MutableSet<String>>()
    private val symbolRefs = ConcurrentHashMap<String, Int>()

    @Synchronized
    override fun subscribe(sessionId: String, symbol: String) {
        val symbols = sessionToSymbols.getOrPut(sessionId) { mutableSetOf() }
        if (!symbols.add(symbol)) return  // 이미 구독 중, no-op

        val newCount = (symbolRefs[symbol] ?: 0) + 1
        val wasFirst = newCount == 1
        symbolRefs[symbol] = newCount

        if (wasFirst) {
            // 새 심볼 → cap 검증 (지금 구독 중인 unique symbol 수 기준)
            if (symbolRefs.size > properties.subscriptionCap) {
                symbolRefs.remove(symbol)
                symbols.remove(symbol)
                throw SubscriptionCapExceededException(properties.subscriptionCap)
            }
            log.info("upstream subscribe symbol={} (first ref)", symbol)
            marketFeedPort.subscribe(symbol)
        }
    }

    @Synchronized
    override fun unsubscribe(sessionId: String, symbol: String) {
        val symbols = sessionToSymbols[sessionId] ?: return
        if (!symbols.remove(symbol)) return

        val newCount = (symbolRefs[symbol] ?: 0) - 1
        if (newCount <= 0) {
            symbolRefs.remove(symbol)
            log.info("upstream unsubscribe symbol={} (last ref released)", symbol)
            marketFeedPort.unsubscribe(symbol)
        } else {
            symbolRefs[symbol] = newCount
        }
        if (symbols.isEmpty()) sessionToSymbols.remove(sessionId)
    }

    @Synchronized
    override fun unsubscribeAll(sessionId: String) {
        val symbols = sessionToSymbols.remove(sessionId) ?: return
        symbols.forEach { symbol ->
            val newCount = (symbolRefs[symbol] ?: 0) - 1
            if (newCount <= 0) {
                symbolRefs.remove(symbol)
                marketFeedPort.unsubscribe(symbol)
            } else {
                symbolRefs[symbol] = newCount
            }
        }
        log.info("session cleanup sessionId={} freed={}", sessionId, symbols.size)
    }
}
