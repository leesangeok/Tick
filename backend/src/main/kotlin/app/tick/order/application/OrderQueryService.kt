package app.tick.order.application

import app.tick.account.application.LoadAccountPort
import app.tick.account.application.LoadStockSnapshotPort
import app.tick.common.exception.AccountNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderQueryService(
    private val loadAccountPort: LoadAccountPort,
    private val loadOrderHistoryPort: LoadOrderHistoryPort,
    private val loadStockSnapshotPort: LoadStockSnapshotPort,
) : GetOrdersUseCase {

    override fun list(memberId: Long): List<OrderListItem> {
        val account = loadAccountPort.loadByMemberId(memberId)
            ?: throw AccountNotFoundException(memberId)
        val orders = loadOrderHistoryPort.loadAllByAccountIdDesc(account.id)
        return orders.map { o ->
            OrderListItem(
                id = "ord_${o.id}",
                symbol = o.stockCode.value,
                stockName = loadStockSnapshotPort.nameOf(o.stockCode) ?: o.stockCode.value,
                side = o.side.name,
                orderType = o.orderType.name,
                quantity = o.quantity.toInt,
                price = o.price.value.toInt(),
                filledQuantity = o.filledQuantity?.toInt,
                status = o.status.name,
                averageCostAt = o.averageCostAt?.value?.toInt(),
                realizedProfitLoss = o.realizedProfitLoss?.value,
                createdAt = o.createdAt.toString(),
                filledAt = o.filledAt?.toString(),
            )
        }
    }
}
