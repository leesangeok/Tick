package app.tick.order.adapter.stock

import app.tick.common.domain.Money
import app.tick.common.domain.StockCode
import app.tick.order.application.LoadStockSummaryPort
import app.tick.stock.StockMasterRepository
import app.tick.stock.StockPriceGenerator
import org.springframework.stereotype.Component

@Component
class StockSummaryAdapter(
    private val stockMasterRepository: StockMasterRepository,
    private val stockPriceGenerator: StockPriceGenerator,
) : LoadStockSummaryPort {

    override fun exists(stockCode: StockCode): Boolean =
        stockMasterRepository.existsById(stockCode.value)

    override fun nameOf(stockCode: StockCode): String? =
        stockMasterRepository.findById(stockCode.value).map { it.name }.orElse(null)

    override fun currentPrice(stockCode: StockCode): Money? {
        val master = stockMasterRepository.findById(stockCode.value).orElse(null) ?: return null
        val series = stockPriceGenerator.generate(master.symbol, master.basePrice, 2)
        return Money.ofInt(series.last().close)
    }
}
