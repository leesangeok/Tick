package app.tick.account

import app.tick.stock.StockService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PortfolioService(
    private val accountRepository: AccountRepository,
    private val holdingRepository: HoldingRepository,
    private val orderHistoryRepository: OrderHistoryRepository,
    private val stockService: StockService,
) {
    fun getPortfolio(memberId: Long): PortfolioResponse {
        val account = accountRepository.findByMemberId(memberId)
            ?: error("Account not found for member: $memberId")
        val holdings = holdingRepository.findAllByAccountId(account.id)
        val stocks = stockService.listAll().associateBy { it.symbol }

        val holdingDtos = holdings.map { h ->
            val stock = stocks[h.symbol] ?: error("Stock not found: ${h.symbol}")
            val evaluationAmount = stock.currentPrice.toLong() * h.quantity
            val costAmount = h.averagePrice.toLong() * h.quantity
            val profitLoss = evaluationAmount - costAmount
            val profitRate = if (costAmount > 0) {
                profitLoss.toDouble() / costAmount.toDouble() * 100.0
            } else {
                0.0
            }
            HoldingResponse(
                symbol = h.symbol,
                name = stock.name,
                quantity = h.quantity,
                averagePrice = h.averagePrice,
                currentPrice = stock.currentPrice,
                evaluationAmount = evaluationAmount,
                profitLoss = profitLoss,
                profitRate = profitRate,
            )
        }

        val evaluationAmount = holdingDtos.sumOf { it.evaluationAmount }
        val holdingsCost = holdingDtos.sumOf { it.averagePrice.toLong() * it.quantity }
        val unrealizedProfitLoss = evaluationAmount - holdingsCost
        val unrealizedProfitRate = if (holdingsCost > 0) {
            unrealizedProfitLoss.toDouble() / holdingsCost.toDouble() * 100.0
        } else {
            0.0
        }
        val realizedProfitLoss = orderHistoryRepository
            .findAllByAccountIdOrderByCreatedAtDesc(account.id)
            .sumOf { it.realizedProfitLoss ?: 0L }
        val totalProfitLoss = unrealizedProfitLoss + realizedProfitLoss
        val totalProfitRate = if (account.totalDeposits > 0) {
            totalProfitLoss.toDouble() / account.totalDeposits.toDouble() * 100.0
        } else {
            0.0
        }
        val todayProfitLoss = holdingDtos.sumOf {
            (stocks[it.symbol]?.changeAmount ?: 0).toLong() * it.quantity
        }
        val todayProfitRate = if (evaluationAmount > 0) {
            todayProfitLoss.toDouble() / evaluationAmount.toDouble() * 100.0
        } else {
            0.0
        }

        return PortfolioResponse(
            cash = account.cash,
            totalDeposits = account.totalDeposits,
            totalAssets = account.cash + evaluationAmount,
            evaluationAmount = evaluationAmount,
            holdingsCost = holdingsCost,
            unrealizedProfitLoss = unrealizedProfitLoss,
            unrealizedProfitRate = unrealizedProfitRate,
            realizedProfitLoss = realizedProfitLoss,
            totalProfitLoss = totalProfitLoss,
            totalProfitRate = totalProfitRate,
            todayProfitLoss = todayProfitLoss,
            todayProfitRate = todayProfitRate,
            holdings = holdingDtos,
        )
    }
}
