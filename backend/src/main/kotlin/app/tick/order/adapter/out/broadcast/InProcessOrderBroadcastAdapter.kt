package app.tick.order.adapter.out.broadcast

import app.tick.order.application.OrderEventPublisherPort
import app.tick.order.application.OrderExecutedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 단일 backend 인스턴스 내 in-memory fan-out. market broadcast 와 같은 패턴이지만 fan-out 키가
 * 심볼이 아니라 memberId — 주문은 개인 데이터이므로 본인 세션에만 보낸다.
 *
 * Redis Pub/Sub 로 확장 시 이 클래스를 `RedisOrderBroadcastAdapter` 로 교체.
 * 다중 인스턴스 배포에선 memberId 소유 인스턴스가 다를 수 있으므로 Pub/Sub 필수.
 */
@Component
class InProcessOrderBroadcastAdapter(
    private val objectMapper: ObjectMapper,
) : OrderEventPublisherPort {
    private val log = LoggerFactory.getLogger(javaClass)

    private val memberIdToSessions = ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>>()

    fun attach(memberId: Long, session: WebSocketSession) {
        memberIdToSessions.getOrPut(memberId) { CopyOnWriteArraySet() }.add(session)
    }

    fun detach(memberId: Long, session: WebSocketSession) {
        memberIdToSessions[memberId]?.let { set ->
            set.remove(session)
            if (set.isEmpty()) memberIdToSessions.remove(memberId, set)
        }
    }

    /** 세션 종료 시 안전 정리. session 만 알아도 모든 memberId 슬롯에서 제거. */
    fun detachAll(session: WebSocketSession) {
        memberIdToSessions.forEach { (_, set) -> set.remove(session) }
        memberIdToSessions.entries.removeIf { it.value.isEmpty() }
    }

    override fun publish(event: OrderExecutedEvent) {
        val sessions = memberIdToSessions[event.memberId] ?: return
        if (sessions.isEmpty()) return
        val payload = try {
            objectMapper.writeValueAsString(
                mapOf(
                    "type" to "order",
                    "orderId" to event.orderId,
                    "symbol" to event.symbol,
                    "stockName" to event.stockName,
                    "side" to event.side,
                    "orderType" to event.orderType,
                    "quantity" to event.quantity,
                    "price" to event.price,
                    "totalAmount" to event.totalAmount,
                    "realizedProfitLoss" to event.realizedProfitLoss,
                    "status" to event.status,
                    "at" to event.at,
                ),
            )
        } catch (e: Exception) {
            log.warn("failed to serialize order event orderId={} err={}", event.orderId, e.message)
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
