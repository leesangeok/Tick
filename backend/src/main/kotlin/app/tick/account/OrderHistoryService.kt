package app.tick.account

import app.tick.stock.StockMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderHistoryService(
    private val accountRepository: AccountRepository,
    private val orderHistoryRepository: OrderHistoryRepository,
    private val stockMasterRepository: StockMasterRepository,
) {
    fun list(memberId: Long): List<OrderResponse> {
        val account = accountRepository.findByMemberId(memberId)
            ?: error("Account not found for member: $memberId")
        val orders = orderHistoryRepository
            .findAllByAccountIdOrderByCreatedAtDesc(account.id)
        val masters = stockMasterRepository.findAll().associateBy { it.symbol }
        return orders.map { o ->
            OrderResponse(
                id = "ord_${o.id}",
                symbol = o.symbol,
                stockName = masters[o.symbol]?.name ?: o.symbol,
                side = o.side,
                orderType = o.orderType,
                quantity = o.quantity,
                price = o.price,
                filledQuantity = o.filledQuantity,
                status = o.status,
                averageCostAt = o.averageCostAt,
                realizedProfitLoss = o.realizedProfitLoss,
                createdAt = o.createdAt.toString(),
                filledAt = o.filledAt?.toString(),
            )
        }
    }
}
