package app.tick.market.adapter.out.broadcast

import app.tick.market.application.port.out.BroadcastPort
import app.tick.market.domain.PriceTick
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 단일 backend 인스턴스 내에서 in-memory fan-out.
 *
 * - `symbolToSessions`: 심볼별로 붙어있는 프론트 WS 세션들. 브로드캐스트 시 iterate.
 * - `attach/detach`: WS handler 가 세션 open/close 시점에 자기 세션을 등록/해제.
 *
 * Redis Pub/Sub 로 확장 시 이 클래스를 `RedisPubSubBroadcastAdapter` 로 교체.
 * `BroadcastPort` 시그니처는 변경 불필요.
 *
 * Thread-safety: `CopyOnWriteArraySet` — broadcast 는 read-heavy, subscribe 변경은 드묾.
 * broadcast 시 세션 send() 는 개별 try/catch 로 감싸서 한 세션 실패가 다른 세션 전파 방해 X.
 */
@Component
class InProcessBroadcastAdapter(
    private val objectMapper: ObjectMapper,
) : BroadcastPort {
    private val log = LoggerFactory.getLogger(javaClass)

    private val symbolToSessions = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>>()

    /** 프론트 WS handler 가 subscribe 요청 처리 시 호출. */
    fun attach(symbol: String, session: WebSocketSession) {
        symbolToSessions.getOrPut(symbol) { CopyOnWriteArraySet() }.add(session)
    }

    fun detach(symbol: String, session: WebSocketSession) {
        symbolToSessions[symbol]?.let { set ->
            set.remove(session)
            if (set.isEmpty()) symbolToSessions.remove(symbol, set)
        }
    }

    /** 세션 종료 시 모든 심볼에서 정리. */
    fun detachAll(session: WebSocketSession) {
        symbolToSessions.forEach { (_, set) -> set.remove(session) }
        symbolToSessions.entries.removeIf { it.value.isEmpty() }
    }

    override fun broadcast(tick: PriceTick) {
        val sessions = symbolToSessions[tick.symbol] ?: return
        if (sessions.isEmpty()) return
        val payload = try {
            objectMapper.writeValueAsString(
                mapOf(
                    "type" to "tick",
                    "symbol" to tick.symbol,
                    "price" to tick.price,
                    "changeAmount" to tick.changeAmount,
                    "changeRate" to tick.changeRate,
                    "volume" to tick.volume,
                    "at" to tick.at.toString(),
                ),
            )
        } catch (e: Exception) {
            log.warn("failed to serialize tick symbol={} err={}", tick.symbol, e.message)
            return
        }
        val msg = TextMessage(payload)
        sessions.forEach { session ->
            try {
                if (session.isOpen) session.sendMessage(msg)
            } catch (e: Exception) {
                log.debug("send failed sessionId={} err={}", session.id, e.message)
            }
        }
    }
}
