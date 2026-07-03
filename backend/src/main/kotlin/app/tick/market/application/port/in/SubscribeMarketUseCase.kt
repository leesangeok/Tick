package app.tick.market.application.port.`in`

/**
 * 프론트 세션 하나가 특정 종목의 실시간 tick 구독을 요청.
 *
 * 세션과 종목 구독은 N:M — 한 세션이 여러 종목을 볼 수 있고, 한 종목을 여러 세션이 볼 수 있음.
 * KIS 세션은 인스턴스당 1개만 유지하므로 upstream 구독은 종목당 1회.
 */
interface SubscribeMarketUseCase {
    /**
     * @throws SubscriptionCapExceededException KIS 세션당 tr_key 최대 (기본 40) 초과 시.
     */
    fun subscribe(sessionId: String, symbol: String)
}

interface UnsubscribeMarketUseCase {
    fun unsubscribe(sessionId: String, symbol: String)
    /** 프론트 WS 세션이 종료되면 세션이 잡고 있던 모든 구독 정리. */
    fun unsubscribeAll(sessionId: String)
}

class SubscriptionCapExceededException(val cap: Int) :
    RuntimeException("KIS WS session cap exceeded (max=$cap)")
