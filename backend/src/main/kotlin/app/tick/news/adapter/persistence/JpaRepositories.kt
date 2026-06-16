package app.tick.news.adapter.persistence

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository

interface NewsJpaRepository : JpaRepository<NewsJpaEntity, Long> {
    fun findAllBySymbolOrderByPublishedAtDesc(symbol: String, limit: Limit): List<NewsJpaEntity>
    fun existsByContentHash(contentHash: String): Boolean
}
