package app.tick.account.application

import app.tick.account.domain.Account
import app.tick.common.domain.Money

interface GetAccountUseCase {
    fun get(memberId: Long): AccountResult
}

interface DepositUseCase {
    fun deposit(command: DepositCommand): AccountResult
}

interface GetPortfolioUseCase {
    fun get(memberId: Long): PortfolioResult
}

interface GetTransactionsUseCase {
    fun list(memberId: Long): List<TransactionResult>
}

interface ProvisionAccountUseCase {
    fun ensureFor(memberId: Long, externalId: String, welcomeBonus: Money): Account
}

data class DepositCommand(val memberId: Long, val amount: Money)

data class AccountResult(
    val id: Long,
    val cash: Long,
    val totalDeposits: Long,
    val realizedProfitLoss: Long,
)

data class HoldingResult(
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: Int,
    val currentPrice: Int,
    val evaluationAmount: Long,
    val profitLoss: Long,
    val profitRate: Double,
)

data class PortfolioResult(
    val cash: Long,
    val totalDeposits: Long,
    val totalAssets: Long,
    val evaluationAmount: Long,
    val holdingsCost: Long,
    val unrealizedProfitLoss: Long,
    val unrealizedProfitRate: Double,
    val realizedProfitLoss: Long,
    val totalProfitLoss: Long,
    val totalProfitRate: Double,
    val todayProfitLoss: Long,
    val todayProfitRate: Double,
    val holdings: List<HoldingResult>,
)

data class TransactionResult(
    val id: String,
    val type: String,
    val amount: Long,
    val symbol: String?,
    val stockName: String?,
    val quantity: Int?,
    val price: Int?,
    val realizedProfitLoss: Long?,
    val createdAt: String,
)
