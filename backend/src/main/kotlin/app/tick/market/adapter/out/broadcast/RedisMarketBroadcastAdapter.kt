package app.tick.market.adapter.out.broadcast

import app.tick.market.application.port.out.BroadcastPort
import app.tick.market.domain.PriceTick
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.connection.Message
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
 * Redis Pub/Sub 기반 fan-out — 다중 backend 인스턴스에서 동일 tick 을 전파.
 *
 * 세션 레지스트리는 여전히 로컬 (JVM 별). broadcast(tick) 이 호출되면:
 * 1. Redis 채널 `market:tick:{symbol}` 에 publish.
 * 2. 각 인스턴스 (자기 자신 포함) 의 subscriber 가 수신 → deliverLocal 로 로컬 세션에 전달.
 *
 * `tick.market.broadcast.mode=redis` 일 때만 활성. 기본은 InProcessBroadcastAdapter.
 */
@Component
@ConditionalOnProperty(
    prefix = "tick.market.broadcast",
    name = ["mode"],
    havingValue = "redis",
)
class RedisMarketBroadcastAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val listenerContainer: RedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
) : BroadcastPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val symbolToSessions = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>>()

    override fun attach(symbol: String, session: WebSocketSession) {
        symbolToSessions.getOrPut(symbol) { CopyOnWriteArraySet() }.add(session)
    }

    override fun detach(symbol: String, session: WebSocketSession) {
        symbolToSessions[symbol]?.let { set ->
            set.remove(session)
            if (set.isEmpty()) symbolToSessions.remove(symbol, set)
        }
    }

    override fun detachAll(session: WebSocketSession) {
        symbolToSessions.forEach { (_, set) -> set.remove(session) }
        symbolToSessions.entries.removeIf { it.value.isEmpty() }
    }

    override fun broadcast(tick: PriceTick) {
        // 자기 자신도 subscribe 중이므로 이 publish 가 곧바로 로컬 delivery 로 이어짐.
        try {
            redisTemplate.convertAndSend(channel(tick.symbol), objectMapper.writeValueAsString(tick))
        } catch (e: Exception) {
            log.warn("redis publish failed symbol={} err={}", tick.symbol, e.message)
        }
    }

    @PostConstruct
    fun subscribe() {
        val adapter = MessageListenerAdapter(this, "onRedisMessage")
        adapter.afterPropertiesSet()
        listenerContainer.addMessageListener(adapter, PatternTopic(CHANNEL_PATTERN))
        log.info("subscribed to redis pattern={}", CHANNEL_PATTERN)
    }

    /** MessageListenerAdapter reflection 진입점. name 은 위 adapter 등록의 methodName 과 일치. */
    fun onRedisMessage(body: String, pattern: String) {
        try {
            val tick: PriceTick = objectMapper.readValue(body)
            deliverLocal(tick)
        } catch (e: Exception) {
            log.warn("redis message parse failed pattern={} err={}", pattern, e.message)
        }
    }

    private fun deliverLocal(tick: PriceTick) {
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

    // MessageListener 인터페이스 시그니처 매칭용 (사용 안 하지만 호환성).
    fun onMessage(message: Message, pattern: ByteArray?) {
        onRedisMessage(String(message.body), pattern?.let { String(it) } ?: "")
    }

    companion object {
        private const val CHANNEL_PATTERN = "market:tick:*"
        fun channel(symbol: String) = "market:tick:$symbol"
    }
}
