package app.tick.market.config

import app.tick.market.adapter.`in`.websocket.MarketWebSocketHandler
import app.tick.market.application.MarketProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * `MarketProperties` 를 무조건 등록 (MarketService 가 tick.market.enabled 값과 무관하게 의존).
 */
@Configuration
@EnableConfigurationProperties(MarketProperties::class)
class MarketPropertiesConfig

/**
 * `/ws/market` 엔드포인트 등록. 프론트가 여기 붙음.
 *
 * `setAllowedOriginPatterns("*")` 은 CorsConfig 의 `tick.cors.allowed-origins` 대신 여기서
 * 별도 관리 — Spring 의 WebSocketHandshake 는 별도 CORS 체크 경로를 씀. 프로덕션에선
 * 실제 도메인만 허용하도록 좁혀야 함 (TODO Phase 4 마무리 시점).
 *
 * `tick.market.enabled=false` 일 때 bean 미등록 → 엔드포인트 자체가 안 열림.
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(prefix = "tick.market", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class MarketWebSocketConfig(
    private val handler: MarketWebSocketHandler,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/market")
            .setAllowedOriginPatterns("*")
    }
}
