package app.tick.account

import app.tick.stock.StockMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TransactionService(
    private val accountRepository: AccountRepository,
    private val orderHistoryRepository: OrderHistoryRepository,
    private val depositHistoryRepository: DepositHistoryRepository,
    private val stockMasterRepository: StockMasterRepository,
) {
    fun list(memberId: Long): List<TransactionResponse> {
        val account = accountRepository.findByMemberId(memberId)
            ?: error("Account not found for member: $memberId")
        val deposits = depositHistoryRepository
            .findAllByAccountIdOrderByCreatedAtDesc(account.id)
        val orders = orderHistoryRepository
            .findAllByAccountIdOrderByCreatedAtDesc(account.id)
            .filter { it.status == "FILLED" }
        val masters = stockMasterRepository.findAll().associateBy { it.symbol }

        val depositTxs = deposits.map { d ->
            TransactionResponse(
                id = "tx_dep_${d.id}",
                type = d.type,
                amount = d.amount,
                symbol = null,
                stockName = null,
                quantity = null,
                price = null,
                realizedProfitLoss = null,
                createdAt = d.createdAt.toString(),
            )
        }

        val orderTxs = orders.map { o ->
            val qty = o.filledQuantity ?: o.quantity
            TransactionResponse(
                id = "tx_ord_${o.id}",
                type = o.side,
                amount = o.price.toLong() * qty,
                symbol = o.symbol,
                stockName = masters[o.symbol]?.name ?: o.symbol,
                quantity = qty,
                price = o.price,
                realizedProfitLoss = o.realizedProfitLoss,
                createdAt = (o.filledAt ?: o.createdAt).toString(),
            )
        }

        return (depositTxs + orderTxs).sortedByDescending { it.createdAt }
    }
}
