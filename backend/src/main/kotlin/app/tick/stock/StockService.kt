package app.tick.stock

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StockService(
    private val repository: StockMasterRepository,
    private val generator: StockPriceGenerator,
) {
    fun listAll(): List<StockResponse> =
        repository.findAll().map(::toResponse)

    fun getBySymbol(symbol: String): StockResponse? =
        repository.findById(symbol).map(::toResponse).orElse(null)

    fun getPriceSeries(symbol: String, days: Int): List<PricePointResponse>? {
        val master = repository.findById(symbol).orElse(null) ?: return null
        return generator.generate(master.symbol, master.basePrice, days).map {
            PricePointResponse(
                timestamp = it.date.toString(),
                open = it.open,
                high = it.high,
                low = it.low,
                close = it.close,
                volume = it.volume,
            )
        }
    }

    private fun toResponse(master: StockMaster): StockResponse {
        val series = generator.generate(master.symbol, master.basePrice, 60)
        val last = series.last()
        val prev = series[series.size - 2]
        val changeAmount = last.close - prev.close
        val changeRate = if (prev.close != 0) changeAmount.toDouble() / prev.close * 100.0 else 0.0
        return StockResponse(
            symbol = master.symbol,
            name = master.name,
            market = master.market,
            sector = master.sector,
            currentPrice = last.close,
            changeAmount = changeAmount,
            changeRate = changeRate,
            volume = last.volume,
        )
    }
}
