package app.tick.order.adapter.out.broadcast

import app.tick.order.application.OrderEventPublisherPort
import app.tick.order.application.OrderExecutedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Redis Pub/Sub 기반 주문 이벤트 fan-out. 다중 backend 인스턴스에서 memberId 소유가 어느
 * 인스턴스인지 몰라도 안전. 각 인스턴스는 자기 로컬 세션에만 delivery.
 */
@Component
@ConditionalOnProperty(
    prefix = "tick.orders.broadcast",
    name = ["mode"],
    havingValue = "redis",
)
class RedisOrderBroadcastAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val listenerContainer: RedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
) : OrderEventPublisherPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val memberIdToSessions = ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>>()

    override fun attach(memberId: Long, session: WebSocketSession) {
        memberIdToSessions.getOrPut(memberId) { CopyOnWriteArraySet() }.add(session)
    }

    override fun detach(memberId: Long, session: WebSocketSession) {
        memberIdToSessions[memberId]?.let { set ->
            set.remove(session)
            if (set.isEmpty()) memberIdToSessions.remove(memberId, set)
        }
    }

    override fun detachAll(session: WebSocketSession) {
        memberIdToSessions.forEach { (_, set) -> set.remove(session) }
        memberIdToSessions.entries.removeIf { it.value.isEmpty() }
    }

    override fun publish(event: OrderExecutedEvent) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event))
        } catch (e: Exception) {
            log.warn("redis publish failed orderId={} err={}", event.orderId, e.message)
        }
    }

    @PostConstruct
    fun subscribe() {
        val adapter = MessageListenerAdapter(this, "onRedisMessage")
        adapter.afterPropertiesSet()
        listenerContainer.addMessageListener(adapter, PatternTopic(CHANNEL))
        log.info("subscribed to redis channel={}", CHANNEL)
    }

    fun onRedisMessage(body: String, pattern: String) {
        try {
            val event: OrderExecutedEvent = objectMapper.readValue(body)
            deliverLocal(event)
        } catch (e: Exception) {
            log.warn("redis order event parse failed pattern={} err={}", pattern, e.message)
        }
    }

    private fun deliverLocal(event: OrderExecutedEvent) {
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

    companion object {
        private const val CHANNEL = "orders:executed"
    }
}
