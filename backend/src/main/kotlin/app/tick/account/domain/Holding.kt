package app.tick.account.domain

import app.tick.common.domain.AveragePriceCalculator
import app.tick.common.domain.Money
import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode
import app.tick.common.exception.InsufficientStockQuantityException
import java.time.Instant

class Holding(
    val id: Long,
    val accountId: Long,
    val stockCode: StockCode,
    quantity: Quantity,
    averagePrice: Money,
    val createdAt: Instant,
    updatedAt: Instant,
) {
    var quantity: Quantity = quantity
        private set
    var averagePrice: Money = averagePrice
        private set
    var updatedAt: Instant = updatedAt
        private set

    fun applyBuy(buyQuantity: Quantity, buyPrice: Money, at: Instant = Instant.now()) {
        averagePrice = AveragePriceCalculator.afterBuy(
            currentQuantity = quantity,
            currentAveragePrice = averagePrice,
            buyQuantity = buyQuantity,
            buyPrice = buyPrice,
        )
        quantity += buyQuantity
        updatedAt = at
    }

    fun applySell(sellQuantity: Quantity, at: Instant = Instant.now()) {
        if (quantity < sellQuantity) {
            throw InsufficientStockQuantityException(sellQuantity.toInt, quantity.toInt)
        }
        quantity = quantity.minus(sellQuantity)
        updatedAt = at
    }

    companion object {
        fun newFromBuy(
            accountId: Long,
            stockCode: StockCode,
            buyQuantity: Quantity,
            buyPrice: Money,
            at: Instant = Instant.now(),
        ): Holding = Holding(
            id = 0L,
            accountId = accountId,
            stockCode = stockCode,
            quantity = buyQuantity,
            averagePrice = buyPrice,
            createdAt = at,
            updatedAt = at,
        )
    }
}
