package app.tick.market.application.port.out

import app.tick.market.domain.PriceTick
import org.springframework.web.socket.WebSocketSession

/**
 * upstream (KIS) 에서 받은 tick 을 프론트 세션들에게 전파하는 포트.
 *
 * 구현체 스위치 (`tick.market.broadcast.mode`):
 * - `inprocess` (기본) — 단일 backend JVM 내 fan-out.
 * - `redis` — Redis Pub/Sub 으로 다른 인스턴스에도 전파 후 각자 local 세션으로 fan-out.
 *
 * attach/detach 는 원래 인터페이스에서 뺐던 세션 API — 두 구현체 모두 로컬 세션 레지스트리를
 * 갖고 있어야 하므로 포트로 승격. 이래야 handler 가 concrete 가 아닌 port 에 의존할 수 있다.
 */
interface BroadcastPort {
    /** 이 종목을 구독 중인 모든 세션에 tick 전송 (redis 모드면 다른 인스턴스에도 propagate). */
    fun broadcast(tick: PriceTick)

    /** WS handler subscribe 시 호출. 이 세션이 해당 심볼 tick 을 받게 됨. */
    fun attach(symbol: String, session: WebSocketSession)

    /** WS handler unsubscribe 시 호출. */
    fun detach(symbol: String, session: WebSocketSession)

    /** 세션 종료 시 모든 심볼에서 정리. */
    fun detachAll(session: WebSocketSession)
}
