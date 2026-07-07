package app.tick.order.adapter.`in`.websocket

import app.tick.auth.AuthPrincipal
import app.tick.order.adapter.out.broadcast.InProcessOrderBroadcastAdapter
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 프론트가 `/ws/orders` 로 붙는 handler. 시세 WS 와 달리 subscribe 프로토콜이 없다 — 접속만
 * 하면 본인 주문 이벤트를 자동 수신 (memberId 로 라우팅).
 *
 * 백엔드 → 프론트 payload:
 * ```
 * {"type":"order","orderId":"ord_1","symbol":"005930","stockName":"삼성전자",
 *  "side":"BUY","orderType":"MARKET","quantity":10,"price":71000,"totalAmount":710000,
 *  "realizedProfitLoss":null,"status":"FILLED","at":"..."}
 * ```
 *
 * 인증: HTTP handshake 시 `SecurityConfig` 의 `JwtAuthenticationFilter` 가 JWT 를 검증하고
 * `SecurityContextHolder` 에 `AuthPrincipal` 을 심는다. 그 결과가 `session.principal` 로 전달됨.
 * principal 을 확인 못하면 즉시 연결 종료.
 */
@Component
class OrderWebSocketHandler(
    private val broadcast: InProcessOrderBroadcastAdapter,
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val memberId = memberIdOf(session)
        if (memberId == null) {
            log.info("orders WS reject: no principal id={}", session.id)
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthenticated"))
            return
        }
        broadcast.attach(memberId, session)
        log.info("orders WS opened id={} memberId={}", session.id, memberId)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        broadcast.detachAll(session)
        log.info("orders WS closed id={} code={}", session.id, status.code)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.debug("orders WS transport error id={} err={}", session.id, exception.message)
    }

    private fun memberIdOf(session: WebSocketSession): Long? {
        val principal = session.principal ?: return null
        val auth = principal as? Authentication ?: return null
        val details = auth.principal as? AuthPrincipal ?: return null
        return details.memberId
    }
}
