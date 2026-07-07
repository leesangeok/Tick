package app.tick.watchlist.application

import app.tick.common.domain.StockCode
import app.tick.watchlist.domain.Watchlist
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WatchlistService(
    private val loadWatchlist: LoadWatchlistPort,
    private val saveWatchlist: SaveWatchlistPort,
    private val deleteWatchlist: DeleteWatchlistPort,
) : GetWatchlistUseCase, AddToWatchlistUseCase, RemoveFromWatchlistUseCase, GetWatchedSymbolsUseCase {

    @Transactional(readOnly = true)
    override fun get(memberId: Long): List<String> =
        loadWatchlist.loadAllByMemberId(memberId).map { it.stockCode.value }

    @Transactional
    override fun add(memberId: Long, stockCode: StockCode) {
        if (loadWatchlist.existsBy(memberId, stockCode)) return
        saveWatchlist.save(Watchlist.newFor(memberId, stockCode))
    }

    @Transactional
    override fun remove(memberId: Long, stockCode: StockCode) {
        deleteWatchlist.deleteBy(memberId, stockCode)
    }

    @Transactional(readOnly = true)
    override fun allDistinctSymbols(): List<StockCode> =
        loadWatchlist.loadAllDistinctSymbols()
}
