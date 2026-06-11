package app.tick.order.adapter.web

import app.tick.order.application.CreateOrderResult
import app.tick.order.application.OrderListItem

data class CreateOrderRequest(
    val stockCode: String,
    val quantity: Int,
    val orderType: String = "MARKET",
)

data class CreateOrderResponse(
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

data class OrderResponse(
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

fun CreateOrderResult.toResponse() = CreateOrderResponse(
    orderId = orderId,
    symbol = symbol,
    stockName = stockName,
    side = side,
    orderType = orderType,
    quantity = quantity,
    price = price,
    totalAmount = totalAmount,
    realizedProfitLoss = realizedProfitLoss,
    status = status,
    filledAt = filledAt,
)

fun OrderListItem.toResponse() = OrderResponse(
    id = id,
    symbol = symbol,
    stockName = stockName,
    side = side,
    orderType = orderType,
    quantity = quantity,
    price = price,
    filledQuantity = filledQuantity,
    status = status,
    averageCostAt = averageCostAt,
    realizedProfitLoss = realizedProfitLoss,
    createdAt = createdAt,
    filledAt = filledAt,
)
