package app.tick.account

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

data class OrderResponse(
    val id: String,
    val symbol: String,
    val stockName: String,
    val side: String,
    val orderType: String,
    val quantity: Int,
    val price: Int,
    val filledQuantity: Int?,
    val status: String,
    val averageCostAt: Int?,
    val realizedProfitLoss: Long?,
    val createdAt: String,
    val filledAt: String?,
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

data class DepositRequest(val amount: Long)
