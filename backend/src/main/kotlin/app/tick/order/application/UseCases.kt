package app.tick.order.application

import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode
import app.tick.order.domain.OrderType

interface CreateOrderUseCase {
    fun buy(command: CreateBuyOrderCommand): CreateOrderResult
    fun sell(command: CreateSellOrderCommand): CreateOrderResult
}

interface GetOrdersUseCase {
    fun list(memberId: Long): List<OrderListItem>
}

data class CreateBuyOrderCommand(
    val memberId: Long,
    val stockCode: StockCode,
    val quantity: Quantity,
    val orderType: OrderType,
)

data class CreateSellOrderCommand(
    val memberId: Long,
    val stockCode: StockCode,
    val quantity: Quantity,
    val orderType: OrderType,
)

data class CreateOrderResult(
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
    val filledAt: String?,
)

data class OrderListItem(
    val id: String,
    val symbol: String,
    val stockName: String,
    val side: String,
    val orderType: String,
    val quantity: Int,
    val price: Int,
    val filledQuantity: Int?,
    val status: String,
    val averageCostAt: Int?,
    val realizedProfitLoss: Long?,
    val createdAt: String,
    val filledAt: String?,
)
