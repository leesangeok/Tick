package app.tick.market.domain

import java.time.Instant

/**
 * 실시간 시세 tick. KIS `H0STCNT0` (주식 체결) 응답을 도메인 언어로 정규화.
 *
 * Money/ProfitLoss VO 를 안 쓰는 이유: hot path (초당 수천 tick 가능) + 직렬화 대상이라
 * 가벼운 primitive 로 유지. 소비자 (프론트) 표시용 데이터일 뿐 정산/저장 대상 아님.
 */
data class PriceTick(
    /** 6자리 종목 코드. e.g. "005930" */
    val symbol: String,
    /** 현재가 (원 단위 정수). */
    val price: Int,
    /** 전일 종가 대비 변동액 (음수 = 하락). */
    val changeAmount: Int,
    /** 전일 종가 대비 변동률 (%). e.g. -2.07 */
    val changeRate: Double,
    /** 누적 거래량. */
    val volume: Long,
    /** 체결 시각 (KST). */
    val at: Instant,
)
