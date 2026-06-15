package app.tick.watchlist.application

import app.tick.common.domain.StockCode
import app.tick.watchlist.domain.Watchlist

interface LoadWatchlistPort {
    fun loadAllByMemberId(memberId: Long): List<Watchlist>
    fun existsBy(memberId: Long, stockCode: StockCode): Boolean
}

interface SaveWatchlistPort {
    fun save(watchlist: Watchlist): Watchlist
}

interface DeleteWatchlistPort {
    fun deleteBy(memberId: Long, stockCode: StockCode)
}
