package app.tick.market.adapter.out.kis

import app.tick.market.application.MarketProperties
import app.tick.stock.adapter.KisStockQuoteProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * H0STCNT0 pipe-delimited tick 파싱 검증.
 *
 * KIS 문서 H0STCNT0 필드 순서 기준 mock payload. 실제 KIS 응답으로 실장 전 lock.
 * 장 개장 시간에 실제 payload 로 다시 검증 필요.
 */
class KisWebSocketFeedAdapterParseTest {

    private val adapter = KisWebSocketFeedAdapter(
        kisProperties = KisStockQuoteProperties(appKey = "x", appSecret = "y"),
        marketProperties = MarketProperties(),
        broadcastPort = mockk(relaxed = true),
        objectMapper = ObjectMapper(),
    )

    @Test
    fun `H0STCNT0 상승 tick 을 PriceTick 으로 파싱한다`() {
        // 필드 순서 (KIS H0STCNT0):
        // 0=종목, 1=시간, 2=현재가, 3=부호(2상승), 4=대비, 5=대비율, 6=가중평균,
        // 7=시가, 8=고가, 9=저가, 10=매도호가1, 11=매수호가1, 12=체결량, 13=누적량, ...
        val body = listOf(
            "005930",     // 0 종목
            "093015",     // 1 09:30:15
            "71000",      // 2 현재가
            "2",          // 3 상승
            "1500",       // 4 절대값 대비
            "2.16",       // 5 대비율
            "70500.00",   // 6 가중평균
            "69800",      // 7 시가
            "71200",      // 8 고가
            "69500",      // 9 저가
            "71100",      // 10 매도호가1
            "71000",      // 11 매수호가1
            "50",         // 12 체결량
            "12345678",   // 13 누적량
        ).joinToString("^")
        val raw = "0|H0STCNT0|001|$body"

        val tick = adapter.parseTick(raw)

        assertNotNull(tick)
        assertEquals("005930", tick.symbol)
        assertEquals(71000, tick.price)
        assertEquals(1500, tick.changeAmount)
        assertEquals(2.16, tick.changeRate)
        assertEquals(12345678L, tick.volume)
    }

    @Test
    fun `H0STCNT0 하락 tick 은 부호가 음수로 반전된다`() {
        val body = listOf(
            "005930", "153025", "71000", "5", "1500", "2.07",
            "0", "0", "0", "0", "0", "0", "0", "999",
        ).joinToString("^")
        val raw = "0|H0STCNT0|001|$body"

        val tick = adapter.parseTick(raw)

        assertNotNull(tick)
        assertEquals(-1500, tick.changeAmount)
        assertEquals(-2.07, tick.changeRate)
    }

    @Test
    fun `count 이 2 이상이면 가장 최근 tick 을 취한다`() {
        val record1 = listOf(
            "005930", "093015", "70000", "2", "500", "0.72",
            "0", "0", "0", "0", "0", "0", "0", "111",
        )
        val record2 = listOf(
            "005930", "093020", "70200", "2", "700", "1.01",
            "0", "0", "0", "0", "0", "0", "0", "222",
        )
        val raw = "0|H0STCNT0|002|" + (record1 + record2).joinToString("^")

        val tick = adapter.parseTick(raw)

        assertNotNull(tick)
        assertEquals(70200, tick.price)
        assertEquals(222L, tick.volume)
    }

    @Test
    fun `H0STCNT0 이 아닌 tr_id 는 null`() {
        val raw = "0|H0STASP0|001|005930^71000^..."
        assertNull(adapter.parseTick(raw))
    }

    @Test
    fun `pipe 필드가 부족하면 null`() {
        assertNull(adapter.parseTick("0|H0STCNT0"))
    }
}
