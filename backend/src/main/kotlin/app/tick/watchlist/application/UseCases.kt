package app.tick.watchlist.application

import app.tick.common.domain.StockCode

interface GetWatchlistUseCase {
    fun get(memberId: Long): List<String>
}

interface AddToWatchlistUseCase {
    fun add(memberId: Long, stockCode: StockCode)
}

interface RemoveFromWatchlistUseCase {
    fun remove(memberId: Long, stockCode: StockCode)
}

/**
 * 전체 사용자 watchlist 를 합쳐 중복 없이 반환. 스케줄러가 "관심 등록된 종목만 뉴스 수집/임베딩"
 * 하도록 대상 축소에 사용. 사용자 개인정보 (memberId) 는 노출하지 않는다.
 */
interface GetWatchedSymbolsUseCase {
    fun allDistinctSymbols(): List<StockCode>
}
