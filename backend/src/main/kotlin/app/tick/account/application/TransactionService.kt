package app.tick.account.application

import app.tick.account.domain.DepositType
import app.tick.common.exception.AccountNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TransactionService(
    private val loadAccountPort: LoadAccountPort,
    private val loadDepositHistoryPort: LoadDepositHistoryPort,
    private val loadOrderSummaryPort: LoadOrderSummaryPort,
    private val loadStockSnapshotPort: LoadStockSnapshotPort,
) : GetTransactionsUseCase {

    override fun list(memberId: Long): List<TransactionResult> {
        val account = loadAccountPort.loadByMemberId(memberId)
            ?: throw AccountNotFoundException(memberId)
        val deposits = loadDepositHistoryPort.loadAllByAccountIdDesc(account.id)
        val orders = loadOrderSummaryPort.loadFilledOrders(account.id)

        val depositTxs = deposits.map { d ->
            TransactionResult(
                id = "tx_dep_${d.id}",
                type = if (d.type == DepositType.DEPOSIT) "DEPOSIT" else "WITHDRAW",
                amount = d.amount.value,
                symbol = null,
                stockName = null,
                quantity = null,
                price = null,
                realizedProfitLoss = null,
                createdAt = d.createdAt.toString(),
            )
        }

        val orderTxs = orders.map { o ->
            TransactionResult(
                id = "tx_ord_${o.id}",
                type = o.side,
                amount = o.price.toLong() * o.quantity,
                symbol = o.stockCode.value,
                stockName = loadStockSnapshotPort.nameOf(o.stockCode) ?: o.stockCode.value,
                quantity = o.quantity,
                price = o.price,
                realizedProfitLoss = o.realizedProfitLoss,
                createdAt = o.filledAt,
            )
        }

        return (depositTxs + orderTxs).sortedByDescending { it.createdAt }
    }
}
