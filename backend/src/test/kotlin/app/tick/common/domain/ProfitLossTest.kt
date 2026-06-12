package app.tick.common.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfitLossTest {
    @Test
    fun `음수와 양수와 0 모두 허용한다`() {
        assertEquals(-1000L, ProfitLoss.of(-1000L).value)
        assertEquals(1000L, ProfitLoss.of(1000L).value)
        assertEquals(0L, ProfitLoss.ZERO.value)
    }

    @Test
    fun `덧셈과 뺄셈 그리고 unaryMinus 가 동작한다`() {
        assertEquals(ProfitLoss.of(300L), ProfitLoss.of(100L) + ProfitLoss.of(200L))
        assertEquals(ProfitLoss.of(-100L), ProfitLoss.of(100L) - ProfitLoss.of(200L))
        assertEquals(ProfitLoss.of(-500L), -ProfitLoss.of(500L))
    }

    @Test
    fun `between 은 after 에서 before 를 뺀 값을 반환한다`() {
        val before = Money.of(1000L)
        val after = Money.of(1500L)
        assertEquals(ProfitLoss.of(500L), ProfitLoss.between(after, before))
    }

    @Test
    fun `before 가 더 크면 음수 손익`() {
        val before = Money.of(2000L)
        val after = Money.of(1500L)
        assertEquals(ProfitLoss.of(-500L), ProfitLoss.between(after, before))
    }

    @Test
    fun `isPositive isNegative isZero 가 동작한다`() {
        assertTrue(ProfitLoss.of(100L).isPositive)
        assertFalse(ProfitLoss.of(100L).isNegative)

        assertTrue(ProfitLoss.of(-100L).isNegative)
        assertFalse(ProfitLoss.of(-100L).isPositive)

        assertTrue(ProfitLoss.ZERO.isZero)
        assertFalse(ProfitLoss.ZERO.isPositive)
        assertFalse(ProfitLoss.ZERO.isNegative)
    }
}
