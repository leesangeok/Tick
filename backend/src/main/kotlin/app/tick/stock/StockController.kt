package app.tick.stock

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stocks")
class StockController(
    private val stockService: StockService,
) {
    @GetMapping
    fun list(): List<StockResponse> = stockService.listAll()

    @GetMapping("/{symbol}")
    fun get(@PathVariable symbol: String): ResponseEntity<StockResponse> {
        val stock = stockService.getBySymbol(symbol) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(stock)
    }

    @GetMapping("/{symbol}/prices")
    fun prices(
        @PathVariable symbol: String,
        @RequestParam(defaultValue = "60") days: Int,
    ): ResponseEntity<List<PricePointResponse>> {
        val capped = days.coerceIn(1, 365)
        val series = stockService.getPriceSeries(symbol, capped)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(series)
    }
}
