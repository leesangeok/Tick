package app.tick.order.adapter.persistence

import app.tick.account.application.LoadOrderSummaryPort
import app.tick.account.application.OrderSummary
import app.tick.common.domain.StockCode
import app.tick.order.application.LoadOrderHistoryPort
import app.tick.order.application.SaveOrderPort
import app.tick.order.domain.Order
import app.tick.order.domain.OrderStatus
import org.springframework.stereotype.Component

@Component
class OrderPersistenceAdapter(
    private val repository: OrderJpaRepository,
) : SaveOrderPort, LoadOrderHistoryPort, LoadOrderSummaryPort {

    override fun save(order: Order): Order {
        val saved = repository.save(OrderMapper.toEntity(order))
        return OrderMapper.toDomain(saved)
    }

    override fun loadAllByAccountIdDesc(accountId: Long): List<Order> =
        repository.findAllByAccountIdOrderByCreatedAtDesc(accountId).map(OrderMapper::toDomain)

    override fun realizedProfitLossSum(accountId: Long): Long =
        repository.findAllByAccountIdOrderByCreatedAtDesc(accountId)
            .sumOf { it.realizedProfitLoss ?: 0L }

    override fun loadFilledOrders(accountId: Long): List<OrderSummary> =
        repository.findAllByAccountIdOrderByCreatedAtDesc(accountId)
            .filter { it.status == OrderStatus.FILLED.name }
            .map {
                OrderSummary(
                    id = it.id,
                    stockCode = StockCode.of(it.symbol),
                    side = it.side,
                    quantity = it.filledQuantity ?: it.quantity,
                    price = it.price,
                    realizedProfitLoss = it.realizedProfitLoss,
                    filledAt = (it.filledAt ?: it.createdAt).toString(),
                )
            }
}
