package app.tick.account.adapter.stock

import app.tick.account.application.LoadStockSnapshotPort
import app.tick.account.application.StockSnapshot
import app.tick.common.domain.StockCode
import app.tick.stock.StockService
import org.springframework.stereotype.Component

@Component
class StockSnapshotAdapter(
    private val stockService: StockService,
) : LoadStockSnapshotPort {

    override fun loadAll(): List<StockSnapshot> =
        stockService.listAll().map {
            StockSnapshot(
                stockCode = StockCode.of(it.symbol),
                name = it.name,
                currentPrice = it.currentPrice,
                changeAmount = it.changeAmount,
            )
        }

    override fun nameOf(stockCode: StockCode): String? =
        stockService.getBySymbol(stockCode.value)?.name
}
