package app.tick.watchlist.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WatchlistJpaRepository : JpaRepository<WatchlistJpaEntity, Long> {
    fun findAllByMemberIdOrderByCreatedAtDesc(memberId: Long): List<WatchlistJpaEntity>
    fun existsByMemberIdAndSymbol(memberId: Long, symbol: String): Boolean
    fun deleteByMemberIdAndSymbol(memberId: Long, symbol: String): Long

    @Query("SELECT DISTINCT w.symbol FROM WatchlistJpaEntity w")
    fun findAllDistinctSymbols(): List<String>
}
