package app.tick.news.application

import app.tick.common.domain.StockCode
import app.tick.news.domain.News

/**
 * 외부 뉴스 소스 (네이버 검색 API 등) 어댑터가 구현.
 * stockCode 기준 키워드 검색 결과를 domain News (미저장) 로 반환.
 */
interface NewsCollectorPort {
    fun search(stockCode: StockCode, limit: Int): List<News>
}

interface LoadNewsPort {
    fun loadRecent(stockCode: StockCode, limit: Int): List<News>
    fun existsByContentHash(contentHash: String): Boolean
}

interface SaveNewsPort {
    fun save(news: News): News
}
