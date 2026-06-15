package app.tick.watchlist.domain

import app.tick.common.domain.StockCode
import java.time.Instant

data class Watchlist(
    val id: Long?,
    val memberId: Long,
    val stockCode: StockCode,
    val createdAt: Instant,
) {
    companion object {
        fun newFor(memberId: Long, stockCode: StockCode): Watchlist =
            Watchlist(id = null, memberId = memberId, stockCode = stockCode, createdAt = Instant.now())
    }
}
