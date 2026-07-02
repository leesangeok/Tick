package app.tick.market.application.port.out

import app.tick.market.domain.PriceTick

/**
 * upstream (KIS) 에서 받은 tick 을 프론트 세션들에게 전파하는 포트.
 *
 * 현재 구현: in-process fan-out (`InProcessBroadcastAdapter`).
 * 미래: Redis Pub/Sub 어댑터로 교체 시 다중 backend 인스턴스 지원.
 *
 * 세션 정보는 여기서 관리하지 않고 broadcast 시점의 세션 lookup 만 담당한다.
 * 세션 등록/해제는 어댑터가 별도 API 로 노출 (인터페이스 최소화 목적).
 */
interface BroadcastPort {
    /** 이 종목을 구독 중인 모든 세션에 tick 전송. */
    fun broadcast(tick: PriceTick)
}
