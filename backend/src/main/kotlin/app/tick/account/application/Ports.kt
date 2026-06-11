package app.tick.account.application

import app.tick.account.domain.Account
import app.tick.account.domain.Deposit
import app.tick.account.domain.Holding
import app.tick.common.domain.StockCode

interface LoadAccountPort {
    fun loadByMemberId(memberId: Long): Account?
    fun loadByExternalId(externalId: String): Account?
}

interface SaveAccountPort {
    fun save(account: Account): Account
}

interface LoadHoldingPort {
    fun loadAllByAccountId(accountId: Long): List<Holding>
    fun loadByAccountIdAndStockCode(accountId: Long, stockCode: StockCode): Holding?
}

interface SaveHoldingPort {
    fun save(holding: Holding): Holding
}

interface LoadDepositHistoryPort {
    fun loadAllByAccountIdDesc(accountId: Long): List<Deposit>
}

interface SaveDepositPort {
    fun save(deposit: Deposit): Deposit
}

interface LoadStockSnapshotPort {
    fun loadAll(): List<StockSnapshot>
    fun nameOf(stockCode: StockCode): String?
}

data class StockSnapshot(
    val stockCode: StockCode,
    val name: String,
    val currentPrice: Int,
    val changeAmount: Int,
)

interface LoadOrderSummaryPort {
    fun realizedProfitLossSum(accountId: Long): Long
    fun loadFilledOrders(accountId: Long): List<OrderSummary>
}

data class OrderSummary(
    val id: Long,
    val stockCode: StockCode,
    val side: String,
    val quantity: Int,
    val price: Int,
    val realizedProfitLoss: Long?,
    val filledAt: String,
)
