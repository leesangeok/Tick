package app.tick.order.domain

import app.tick.common.domain.Money
import app.tick.common.domain.ProfitLoss
import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode
import java.time.Instant

enum class OrderSide { BUY, SELL }
enum class OrderType { MARKET, LIMIT }
enum class OrderStatus { PENDING, FILLED, CANCELED, REJECTED }

class Order(
    val id: Long,
    val accountId: Long,
    val stockCode: StockCode,
    val side: OrderSide,
    val orderType: OrderType,
    val quantity: Quantity,
    val price: Money,
    val filledQuantity: Quantity?,
    val status: OrderStatus,
    val averageCostAt: Money?,
    val realizedProfitLoss: ProfitLoss?,
    val createdAt: Instant,
    val filledAt: Instant?,
) {
    val totalAmount: Money get() = price.multiply(quantity)

    companion object {
        fun filledBuy(
            accountId: Long,
            stockCode: StockCode,
            quantity: Quantity,
            price: Money,
            orderType: OrderType,
            at: Instant = Instant.now(),
        ): Order = Order(
            id = 0L,
            accountId = accountId,
            stockCode = stockCode,
            side = OrderSide.BUY,
            orderType = orderType,
            quantity = quantity,
            price = price,
            filledQuantity = quantity,
            status = OrderStatus.FILLED,
            averageCostAt = null,
            realizedProfitLoss = null,
            createdAt = at,
            filledAt = at,
        )

        fun filledSell(
            accountId: Long,
            stockCode: StockCode,
            quantity: Quantity,
            price: Money,
            orderType: OrderType,
            averageCostAt: Money,
            realizedProfitLoss: ProfitLoss,
            at: Instant = Instant.now(),
        ): Order = Order(
            id = 0L,
            accountId = accountId,
            stockCode = stockCode,
            side = OrderSide.SELL,
            orderType = orderType,
            quantity = quantity,
            price = price,
            filledQuantity = quantity,
            status = OrderStatus.FILLED,
            averageCostAt = averageCostAt,
            realizedProfitLoss = realizedProfitLoss,
            createdAt = at,
            filledAt = at,
        )
    }
}
