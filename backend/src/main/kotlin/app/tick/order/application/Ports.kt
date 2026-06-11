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
