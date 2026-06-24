package app.tick.stock

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StockService(
    private val repository: StockMasterRepository,
    private val quoteProvider: StockQuoteProvider,
) {
    fun listAll(): List<StockResponse> =
        repository.findAll().map(::toResponse)

    fun getBySymbol(symbol: String): StockResponse? =
        repository.findById(symbol).map(::toResponse).orElse(null)

    fun getPriceSeries(symbol: String, days: Int): List<PricePointResponse>? {
        if (!repository.existsById(symbol)) return null
        return quoteProvider.priceSeries(symbol, days).map {
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
        val quote = quoteProvider.quote(master.symbol)
        return StockResponse(
            symbol = master.symbol,
            name = master.name,
            market = master.market,
            sector = master.sector,
            currentPrice = quote?.currentPrice ?: master.basePrice,
            changeAmount = quote?.changeAmount ?: 0,
            changeRate = quote?.changeRate ?: 0.0,
            volume = quote?.volume ?: 0L,
        )
    }
}
