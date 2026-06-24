package app.tick.stock.adapter

import app.tick.stock.StockMasterRepository
import app.tick.stock.StockPriceGenerator
import app.tick.stock.StockPricePoint
import app.tick.stock.StockQuote
import app.tick.stock.StockQuoteProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 가짜 가격 생성기 (mulberry32 deterministic) 기반의 [StockQuoteProvider].
 *
 * 외부 시세 API 가 불가한 환경 (오프라인 / 테스트 / 데모) 또는 비용/지연을 피하고 싶을 때
 * `tick.stock-quote.provider=mulberry` 로 전환.
 *
 * 프론트엔드 mocks/priceSeries.ts 와 같은 알고리즘이라 양쪽이 같은 시계열을 산출함.
 */
@Component
@ConditionalOnProperty(prefix = "tick.stock-quote", name = ["provider"], havingValue = "mulberry")
class MulberryStockQuoteAdapter(
    private val stockMasterRepository: StockMasterRepository,
    private val stockPriceGenerator: StockPriceGenerator,
) : StockQuoteProvider {

    override fun quote(symbol: String): StockQuote? {
        val master = stockMasterRepository.findById(symbol).orElse(null) ?: return null
        val series = stockPriceGenerator.generate(master.symbol, master.basePrice, 2)
        val last = series.last()
        val prev = series.first()
        val changeAmount = last.close - prev.close
        val changeRate = if (prev.close != 0) changeAmount.toDouble() / prev.close * 100.0 else 0.0
        return StockQuote(
            symbol = master.symbol,
            currentPrice = last.close,
            previousClose = prev.close,
            changeAmount = changeAmount,
            changeRate = changeRate,
            volume = last.volume,
        )
    }

    override fun priceSeries(symbol: String, days: Int): List<StockPricePoint> {
        val master = stockMasterRepository.findById(symbol).orElse(null) ?: return emptyList()
        return stockPriceGenerator.generate(master.symbol, master.basePrice, days).map {
            StockPricePoint(it.date, it.open, it.high, it.low, it.close, it.volume)
        }
    }
}
