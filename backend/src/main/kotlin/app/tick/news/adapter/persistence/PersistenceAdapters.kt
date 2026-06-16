package app.tick.news.adapter.persistence

import app.tick.common.domain.StockCode
import app.tick.news.application.LoadNewsPort
import app.tick.news.application.SaveNewsPort
import app.tick.news.domain.News
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Component

@Component
class NewsPersistenceAdapter(
    private val repository: NewsJpaRepository,
) : LoadNewsPort, SaveNewsPort {

    override fun loadRecent(stockCode: StockCode, limit: Int): List<News> =
        repository.findAllBySymbolOrderByPublishedAtDesc(stockCode.value, Limit.of(limit))
            .map(NewsMapper::toDomain)

    override fun existsByContentHash(contentHash: String): Boolean =
        repository.existsByContentHash(contentHash)

    override fun save(news: News): News =
        NewsMapper.toDomain(repository.save(NewsMapper.toEntity(news)))
}
