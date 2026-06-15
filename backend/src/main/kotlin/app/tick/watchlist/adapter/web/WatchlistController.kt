package app.tick.watchlist.adapter.web

import app.tick.auth.AuthPrincipal
import app.tick.common.domain.StockCode
import app.tick.common.response.ApiResponse
import app.tick.watchlist.application.AddToWatchlistUseCase
import app.tick.watchlist.application.GetWatchlistUseCase
import app.tick.watchlist.application.RemoveFromWatchlistUseCase
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/watchlist")
class WatchlistController(
    private val getWatchlist: GetWatchlistUseCase,
    private val addToWatchlist: AddToWatchlistUseCase,
    private val removeFromWatchlist: RemoveFromWatchlistUseCase,
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: AuthPrincipal): ApiResponse<List<String>> =
        ApiResponse.success(getWatchlist.get(principal.memberId))

    @PostMapping("/{symbol}")
    fun add(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PathVariable symbol: String,
    ): ApiResponse<Unit> {
        addToWatchlist.add(principal.memberId, StockCode(symbol))
        return ApiResponse.success()
    }

    @DeleteMapping("/{symbol}")
    fun remove(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @PathVariable symbol: String,
    ): ApiResponse<Unit> {
        removeFromWatchlist.remove(principal.memberId, StockCode(symbol))
        return ApiResponse.success()
    }
}
