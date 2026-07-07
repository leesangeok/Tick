package app.tick.order.application

import app.tick.common.domain.Money
import app.tick.common.domain.StockCode
import app.tick.order.domain.Order

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
 * OrderService 는 이 port 를 부르기만 하고, 구독자/세션 관리는 adapter 가 담당.
 */
interface OrderEventPublisherPort {
    fun publish(event: OrderExecutedEvent)
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
