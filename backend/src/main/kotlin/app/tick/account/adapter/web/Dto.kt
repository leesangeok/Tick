package app.tick.account.adapter.web

import app.tick.account.application.AccountResult
import app.tick.account.application.HoldingResult
import app.tick.account.application.PortfolioResult
import app.tick.account.application.TransactionResult

data class DepositRequest(val amount: Long)

data class AccountResponse(
    val id: Long,
    val cash: Long,
    val totalDeposits: Long,
    val realizedProfitLoss: Long,
)

data class HoldingResponse(
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: Int,
    val currentPrice: Int,
    val evaluationAmount: Long,
    val profitLoss: Long,
    val profitRate: Double,
)

data class PortfolioResponse(
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
    val holdings: List<HoldingResponse>,
)

data class TransactionResponse(
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

fun AccountResult.toResponse() = AccountResponse(id, cash, totalDeposits, realizedProfitLoss)

fun HoldingResult.toResponse() = HoldingResponse(
    symbol = symbol,
    name = name,
    quantity = quantity,
    averagePrice = averagePrice,
    currentPrice = currentPrice,
    evaluationAmount = evaluationAmount,
    profitLoss = profitLoss,
    profitRate = profitRate,
)

fun PortfolioResult.toResponse() = PortfolioResponse(
    cash = cash,
    totalDeposits = totalDeposits,
    totalAssets = totalAssets,
    evaluationAmount = evaluationAmount,
    holdingsCost = holdingsCost,
    unrealizedProfitLoss = unrealizedProfitLoss,
    unrealizedProfitRate = unrealizedProfitRate,
    realizedProfitLoss = realizedProfitLoss,
    totalProfitLoss = totalProfitLoss,
    totalProfitRate = totalProfitRate,
    todayProfitLoss = todayProfitLoss,
    todayProfitRate = todayProfitRate,
    holdings = holdings.map { it.toResponse() },
)

fun TransactionResult.toResponse() = TransactionResponse(
    id = id,
    type = type,
    amount = amount,
    symbol = symbol,
    stockName = stockName,
    quantity = quantity,
    price = price,
    realizedProfitLoss = realizedProfitLoss,
    createdAt = createdAt,
)
