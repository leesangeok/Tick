package app.tick.watchlist.application

import app.tick.common.domain.StockCode

interface GetWatchlistUseCase {
    fun get(memberId: Long): List<String>
}

interface AddToWatchlistUseCase {
    fun add(memberId: Long, stockCode: StockCode)
}

interface RemoveFromWatchlistUseCase {
    fun remove(memberId: Long, stockCode: StockCode)
}
