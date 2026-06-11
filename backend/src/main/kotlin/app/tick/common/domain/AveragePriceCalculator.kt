package app.tick.common.domain

object AveragePriceCalculator {
    fun afterBuy(
        currentQuantity: Quantity,
        currentAveragePrice: Money,
        buyQuantity: Quantity,
        buyPrice: Money,
    ): Money {
        val currentTotal = currentAveragePrice.value * currentQuantity.value
        val buyTotal = buyPrice.value * buyQuantity.value
        val totalQuantity = currentQuantity.value + buyQuantity.value
        require(totalQuantity > 0) { "매수 후 수량이 0일 수 없습니다." }
        return Money(((currentTotal + buyTotal) + totalQuantity / 2) / totalQuantity)
    }

    fun realizedProfitLoss(
        sellPrice: Money,
        averagePrice: Money,
        sellQuantity: Quantity,
    ): ProfitLoss =
        ProfitLoss((sellPrice.value - averagePrice.value) * sellQuantity.value)
}
