package app.tick.order.adapter.stock

import app.tick.common.domain.Money
import app.tick.common.domain.StockCode
import app.tick.order.application.LoadStockSummaryPort
import app.tick.stock.StockMasterRepository
import app.tick.stock.StockQuoteProvider
import org.springframework.stereotype.Component

@Component
class StockSummaryAdapter(
    private val stockMasterRepository: StockMasterRepository,
    private val stockQuoteProvider: StockQuoteProvider,
) : LoadStockSummaryPort {

    override fun exists(stockCode: StockCode): Boolean =
        stockMasterRepository.existsById(stockCode.value)

    override fun nameOf(stockCode: StockCode): String? =
        stockMasterRepository.findById(stockCode.value).map { it.name }.orElse(null)

    override fun currentPrice(stockCode: StockCode): Money? {
        val quote = stockQuoteProvider.quote(stockCode.value) ?: return null
        return Money.ofInt(quote.currentPrice)
    }
}
