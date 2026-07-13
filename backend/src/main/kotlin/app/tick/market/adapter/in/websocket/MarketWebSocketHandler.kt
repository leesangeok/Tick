package app.tick.market.adapter.`in`.websocket

import app.tick.market.application.port.`in`.SubscribeMarketUseCase
import app.tick.market.application.port.`in`.SubscriptionCapExceededException
import app.tick.market.application.port.`in`.UnsubscribeMarketUseCase
import app.tick.market.application.port.out.BroadcastPort
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 프론트가 `/ws/market` 으로 붙는 handler.
 *
 * 프로토콜 (프론트 → 백엔드):
 * ```
 * {"action":"subscribe","symbol":"005930"}
 * {"action":"unsubscribe","symbol":"005930"}
 * ```
 * 프로토콜 (백엔드 → 프론트):
 * ```
 * {"type":"tick","symbol":"005930","price":71000,"changeAmount":-1500,"changeRate":-2.07,"volume":123456789,"at":"..."}
 * {"type":"ack","action":"subscribe","symbol":"005930"}
 * {"type":"error","message":"..."}
 * ```
 *
 * 세션 종료 시 자동 unsubscribe → KIS 세션당 40개 제한 안 넘게.
 *
 * attach/detach 는 이제 BroadcastPort 에 정의 — 구현체가 InProcess/Redis 로 바뀌어도 handler
 * 코드는 그대로. mode 는 `tick.market.broadcast.mode` 로 결정.
 */
@Component
class MarketWebSocketHandler(
    private val subscribeUseCase: SubscribeMarketUseCase,
    private val unsubscribeUseCase: UnsubscribeMarketUseCase,
    private val broadcast: BroadcastPort,
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("client WS opened id={}", session.id)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val payload = message.payload
        val node: JsonNode = try {
            objectMapper.readTree(payload)
        } catch (e: Exception) {
            sendError(session, "invalid JSON")
            return
        }
        val action = node.path("action").asText(null)
        val symbol = node.path("symbol").asText(null)
        if (symbol.isNullOrBlank() || !symbol.matches(SYMBOL_RE)) {
            sendError(session, "invalid symbol")
            return
        }
        when (action) {
            "subscribe" -> handleSubscribe(session, symbol)
            "unsubscribe" -> handleUnsubscribe(session, symbol)
            else -> sendError(session, "unknown action: $action")
        }
    }

    private fun handleSubscribe(session: WebSocketSession, symbol: String) {
        try {
            subscribeUseCase.subscribe(session.id, symbol)
            broadcast.attach(symbol, session)
            sendAck(session, "subscribe", symbol)
        } catch (e: SubscriptionCapExceededException) {
            sendError(session, "subscription cap exceeded (${e.cap})")
        } catch (e: Exception) {
            log.warn("subscribe failed sessionId={} symbol={} err={}", session.id, symbol, e.message)
            sendError(session, "subscribe failed")
        }
    }

    private fun handleUnsubscribe(session: WebSocketSession, symbol: String) {
        broadcast.detach(symbol, session)
        unsubscribeUseCase.unsubscribe(session.id, symbol)
        sendAck(session, "unsubscribe", symbol)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info("client WS closed id={} code={}", session.id, status.code)
        broadcast.detachAll(session)
        unsubscribeUseCase.unsubscribeAll(session.id)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.debug("client WS transport error id={} err={}", session.id, exception.message)
    }

    private fun sendAck(session: WebSocketSession, action: String, symbol: String) {
        sendJson(session, mapOf("type" to "ack", "action" to action, "symbol" to symbol))
    }

    private fun sendError(session: WebSocketSession, message: String) {
        sendJson(session, mapOf("type" to "error", "message" to message))
    }

    private fun sendJson(session: WebSocketSession, payload: Any) {
        if (!session.isOpen) return
        try {
            session.sendMessage(TextMessage(objectMapper.writeValueAsString(payload)))
        } catch (e: Exception) {
            log.debug("send failed id={} err={}", session.id, e.message)
        }
    }

    companion object {
        private val SYMBOL_RE = Regex("^\\d{6}$")
    }
}
