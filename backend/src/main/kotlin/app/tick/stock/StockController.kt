package app.tick.stock

import app.tick.common.exception.StockNotFoundException
import app.tick.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/stocks")
class StockController(
    private val stockService: StockService,
) {
    @GetMapping
    fun list(): ApiResponse<List<StockResponse>> =
        ApiResponse.success(stockService.listAll())

    @GetMapping("/{symbol}")
    fun get(@PathVariable symbol: String): ApiResponse<StockResponse> =
        ApiResponse.success(stockService.getBySymbol(symbol) ?: throw StockNotFoundException(symbol))

    @GetMapping("/{symbol}/prices")
    fun prices(
        @PathVariable symbol: String,
        @RequestParam(defaultValue = "60") days: Int,
    ): ApiResponse<List<PricePointResponse>> {
        val capped = days.coerceIn(1, 365)
        val series = stockService.getPriceSeries(symbol, capped)
            ?: throw StockNotFoundException(symbol)
        return ApiResponse.success(series)
    }
}
