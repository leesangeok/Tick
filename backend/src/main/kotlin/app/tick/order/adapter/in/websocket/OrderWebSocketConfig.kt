package app.tick.order.adapter.`in`.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * `/ws/orders` 엔드포인트 등록. SecurityConfig 의 `anyRequest().authenticated()` 로 자동 보호됨.
 * setAllowedOriginPatterns("*") 는 prod 배포 시 실제 프론트 도메인만 허용하도록 좁혀야 함.
 */
@Configuration
@EnableWebSocket
class OrderWebSocketConfig(
    private val handler: OrderWebSocketHandler,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/orders")
            .setAllowedOriginPatterns("*")
    }
}
