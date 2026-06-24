package app.tick.stock

import java.time.LocalDate

/**
 * 종목 시세 조회 추상화. 실제 외부 시세 API (Yahoo Finance / 한투 KIS 등) 와 가짜 가격
 * 생성기 (StockPriceGenerator) 둘 다 같은 인터페이스로 노출하기 위한 port.
 *
 * StockService / OrderService / PortfolioService 가 이 port 에만 의존하므로 외부 시세
 * 프로바이더 교체 시 도메인 영향 없음.
 */
interface StockQuoteProvider {
    /**
     * 종목의 현재가 스냅샷. 호출 실패 / 데이터 없음 시 null.
     * 모의투자 환경이라 1~15분 지연 데이터로도 충분.
     */
    fun quote(symbol: String): StockQuote?

    /**
     * 최근 N 일의 일봉 OHLCV. 호출 실패 / 데이터 없음 시 빈 리스트.
     */
    fun priceSeries(symbol: String, days: Int): List<StockPricePoint>
}

data class StockQuote(
    val symbol: String,
    val currentPrice: Int,
    val previousClose: Int,
    val changeAmount: Int,
    val changeRate: Double,
    val volume: Long,
)

data class StockPricePoint(
    val date: LocalDate,
    val open: Int,
    val high: Int,
    val low: Int,
    val close: Int,
    val volume: Long,
)
