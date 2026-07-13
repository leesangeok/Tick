package app.tick.news.adapter.persistence

import app.tick.common.domain.StockCode
import app.tick.news.domain.News

object NewsMapper {
    fun toDomain(entity: NewsJpaEntity): News = News(
        id = entity.id,
        stockCode = StockCode(entity.symbol),
        title = entity.title,
        body = entity.body,
        source = entity.source,
        sourceUrl = entity.sourceUrl,
        publishedAt = entity.publishedAt,
        contentHash = entity.contentHash,
        archiveUrl = entity.archiveUrl,
    )

    fun toEntity(domain: News): NewsJpaEntity = NewsJpaEntity(
        id = domain.id ?: 0,
        symbol = domain.stockCode.value,
        title = domain.title,
        body = domain.body,
        source = domain.source,
        sourceUrl = domain.sourceUrl,
        publishedAt = domain.publishedAt,
        contentHash = domain.contentHash,
        archiveUrl = domain.archiveUrl,
    )
}
