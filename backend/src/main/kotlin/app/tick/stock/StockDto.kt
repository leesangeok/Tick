package app.tick.stock

data class StockResponse(
    val symbol: String,
    val name: String,
    val market: String,
    val sector: String,
    val currentPrice: Int,
    val changeAmount: Int,
    val changeRate: Double,
    val volume: Long,
    val isFavorite: Boolean = false,
)

data class PricePointResponse(
    val timestamp: String,
    val open: Int,
    val high: Int,
    val low: Int,
    val close: Int,
    val volume: Long,
)
