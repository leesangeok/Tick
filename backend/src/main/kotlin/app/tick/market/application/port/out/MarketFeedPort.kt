package app.tick.market.application.port.out

/**
 * 외부 실시간 시세 소스 (현재 구현: KIS WebSocket) 에 대한 upstream 구독 관리 포트.
 *
 * MarketService 가 여러 프론트 세션을 관리하면서 종목별 reference count 를 유지하고,
 * 새 종목 첫 구독 / 마지막 구독 해제 시점에만 이 포트를 호출한다.
 *
 * 어댑터는 KIS 세션 연결/재연결/PINGPONG 응답 등 전송 계층을 캡슐화한다.
 */
interface MarketFeedPort {
    /** upstream 에 해당 종목 구독 등록. 이미 등록돼 있으면 idempotent. */
    fun subscribe(symbol: String)

    /** upstream 구독 해제. */
    fun unsubscribe(symbol: String)
}
