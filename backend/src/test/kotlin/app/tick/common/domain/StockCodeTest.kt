package app.tick.common.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class StockCodeTest {
    @Test
    fun `유효한 종목코드로 생성된다`() {
        assertEquals("005930", StockCode.of("005930").value)
    }

    @Test
    fun `빈 문자열은 거부`() {
        assertThrows<IllegalArgumentException> { StockCode.of("") }
        assertThrows<IllegalArgumentException> { StockCode.of("   ") }
    }

    @Test
    fun `길이가 범위를 벗어나면 거부`() {
        assertThrows<IllegalArgumentException> { StockCode.of("1234") }       // 너무 짧음
        assertThrows<IllegalArgumentException> { StockCode.of("12345678901") } // 너무 김
    }

    @Test
    fun `toString 은 value 와 동일`() {
        assertEquals("005930", StockCode.of("005930").toString())
    }
}
