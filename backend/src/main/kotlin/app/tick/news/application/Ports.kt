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

/**
 * 뉴스 원본 body 를 외부 스토리지 (S3) 에 아카이빙. 실패 시 null 반환 → NewsService 는
 * archive_url 없이 저장 계속. 감사/원문 재확인 목적, RAG 검색 경로에는 영향 없음.
 */
interface NewsArchivePort {
    fun archive(news: News): String?
}
