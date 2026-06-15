package app.tick.watchlist.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface WatchlistJpaRepository : JpaRepository<WatchlistJpaEntity, Long> {
    fun findAllByMemberIdOrderByCreatedAtDesc(memberId: Long): List<WatchlistJpaEntity>
    fun existsByMemberIdAndSymbol(memberId: Long, symbol: String): Boolean
    fun deleteByMemberIdAndSymbol(memberId: Long, symbol: String): Long
}
