package app.tick.watchlist.adapter.persistence

import app.tick.common.domain.StockCode
import app.tick.watchlist.application.DeleteWatchlistPort
import app.tick.watchlist.application.LoadWatchlistPort
import app.tick.watchlist.application.SaveWatchlistPort
import app.tick.watchlist.domain.Watchlist
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WatchlistPersistenceAdapter(
    private val repository: WatchlistJpaRepository,
) : LoadWatchlistPort, SaveWatchlistPort, DeleteWatchlistPort {
    override fun loadAllByMemberId(memberId: Long): List<Watchlist> =
        repository.findAllByMemberIdOrderByCreatedAtDesc(memberId).map(WatchlistMapper::toDomain)

    override fun existsBy(memberId: Long, stockCode: StockCode): Boolean =
        repository.existsByMemberIdAndSymbol(memberId, stockCode.value)

    override fun loadAllDistinctSymbols(): List<StockCode> =
        repository.findAllDistinctSymbols().map(::StockCode)

    override fun save(watchlist: Watchlist): Watchlist =
        WatchlistMapper.toDomain(repository.save(WatchlistMapper.toEntity(watchlist)))

    @Transactional
    override fun deleteBy(memberId: Long, stockCode: StockCode) {
        repository.deleteByMemberIdAndSymbol(memberId, stockCode.value)
    }
}
