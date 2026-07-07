package app.tick.order.application

import app.tick.common.domain.Money
import app.tick.common.domain.StockCode
import app.tick.order.domain.Order
import org.springframework.web.socket.WebSocketSession

interface SaveOrderPort {
    fun save(order: Order): Order
}

interface LoadOrderHistoryPort {
    fun loadAllByAccountIdDesc(accountId: Long): List<Order>
}

interface LoadStockSummaryPort {
    fun exists(stockCode: StockCode): Boolean
    fun nameOf(stockCode: StockCode): String?
    fun currentPrice(stockCode: StockCode): Money?
}

/**
 * 주문 체결 이벤트 발행. 프론트로 실시간 전송 (WS `/ws/orders`) 이 주된 소비자.
 *
 * 구현체 스위치 (`tick.orders.broadcast.mode`):
 * - `inprocess` (기본) — 단일 JVM 내 memberId → sessions 라우팅.
 * - `redis` — Redis Pub/Sub 으로 다른 backend 인스턴스에도 전파.
 *
 * attach/detach 는 두 구현체 모두 로컬 세션 레지스트리를 갖게 되므로 port 로 승격.
 */
interface OrderEventPublisherPort {
    fun publish(event: OrderExecutedEvent)
    fun attach(memberId: Long, session: WebSocketSession)
    fun detach(memberId: Long, session: WebSocketSession)
    fun detachAll(session: WebSocketSession)
}

data class OrderExecutedEvent(
    val memberId: Long,
    val orderId: String,
    val symbol: String,
    val stockName: String,
    val side: String,
    val orderType: String,
    val quantity: Int,
    val price: Int,
    val totalAmount: Long,
    val realizedProfitLoss: Long?,
    val status: String,
    val at: String,
)
