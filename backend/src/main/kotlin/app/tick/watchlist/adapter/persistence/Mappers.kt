package app.tick.watchlist.adapter.persistence

import app.tick.common.domain.StockCode
import app.tick.watchlist.domain.Watchlist

object WatchlistMapper {
    fun toDomain(entity: WatchlistJpaEntity): Watchlist = Watchlist(
        id = entity.id,
        memberId = entity.memberId,
        stockCode = StockCode(entity.symbol),
        createdAt = entity.createdAt,
    )

    fun toEntity(domain: Watchlist): WatchlistJpaEntity = WatchlistJpaEntity(
        id = domain.id ?: 0,
        memberId = domain.memberId,
        symbol = domain.stockCode.value,
        createdAt = domain.createdAt,
    )
}
