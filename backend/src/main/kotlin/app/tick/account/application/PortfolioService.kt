package app.tick.account.application

import app.tick.common.exception.AccountNotFoundException
import app.tick.common.exception.StockNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PortfolioService(
    private val loadAccountPort: LoadAccountPort,
    private val loadHoldingPort: LoadHoldingPort,
    private val loadStockSnapshotPort: LoadStockSnapshotPort,
    private val loadOrderSummaryPort: LoadOrderSummaryPort,
) : GetPortfolioUseCase {

    override fun get(memberId: Long): PortfolioResult {
        val account = loadAccountPort.loadByMemberId(memberId)
            ?: throw AccountNotFoundException(memberId)
        val holdings = loadHoldingPort.loadAllByAccountId(account.id)
        val snapshotsByCode = loadStockSnapshotPort.loadAll().associateBy { it.stockCode }

        val holdingResults = holdings.map { h ->
            val snapshot = snapshotsByCode[h.stockCode] ?: throw StockNotFoundException(h.stockCode.value)
            val qty = h.quantity.value
            val evaluation = snapshot.currentPrice.toLong() * qty
            val cost = h.averagePrice.value * qty
            val profitLoss = evaluation - cost
            val profitRate = if (cost > 0) profitLoss.toDouble() / cost.toDouble() * 100.0 else 0.0
            HoldingResult(
                symbol = h.stockCode.value,
                name = snapshot.name,
                quantity = qty.toInt(),
                averagePrice = h.averagePrice.value.toInt(),
                currentPrice = snapshot.currentPrice,
                evaluationAmount = evaluation,
                profitLoss = profitLoss,
                profitRate = profitRate,
            )
        }

        val evaluationAmount = holdingResults.sumOf { it.evaluationAmount }
        val holdingsCost = holdingResults.sumOf { it.averagePrice.toLong() * it.quantity }
        val unrealizedProfitLoss = evaluationAmount - holdingsCost
        val unrealizedProfitRate = if (holdingsCost > 0) {
            unrealizedProfitLoss.toDouble() / holdingsCost.toDouble() * 100.0
        } else 0.0
        val realizedProfitLoss = loadOrderSummaryPort.realizedProfitLossSum(account.id)
        val totalProfitLoss = unrealizedProfitLoss + realizedProfitLoss
        val totalDeposits = account.totalDeposits.value
        val totalProfitRate = if (totalDeposits > 0) {
            totalProfitLoss.toDouble() / totalDeposits.toDouble() * 100.0
        } else 0.0
        val todayProfitLoss = holdingResults.sumOf { hr ->
            val snapshot = snapshotsByCode[app.tick.common.domain.StockCode.of(hr.symbol)]
            (snapshot?.changeAmount ?: 0).toLong() * hr.quantity
        }
        val todayProfitRate = if (evaluationAmount > 0) {
            todayProfitLoss.toDouble() / evaluationAmount.toDouble() * 100.0
        } else 0.0

        return PortfolioResult(
            cash = account.cash.value,
            totalDeposits = totalDeposits,
            totalAssets = account.cash.value + evaluationAmount,
            evaluationAmount = evaluationAmount,
            holdingsCost = holdingsCost,
            unrealizedProfitLoss = unrealizedProfitLoss,
            unrealizedProfitRate = unrealizedProfitRate,
            realizedProfitLoss = realizedProfitLoss,
            totalProfitLoss = totalProfitLoss,
            totalProfitRate = totalProfitRate,
            todayProfitLoss = todayProfitLoss,
            todayProfitRate = todayProfitRate,
            holdings = holdingResults,
        )
    }
}
