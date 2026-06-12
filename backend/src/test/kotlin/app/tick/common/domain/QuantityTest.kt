package app.tick.common.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantityTest {
    @Test
    fun `0 또는 양수로 생성된다`() {
        assertEquals(0L, Quantity.ZERO.value)
        assertEquals(10L, Quantity.ofInt(10).value)
    }

    @Test
    fun `음수로 생성하면 예외`() {
        assertThrows<IllegalArgumentException> { Quantity.of(-1L) }
    }

    @Test
    fun `positive 팩토리는 0 이하를 거부한다`() {
        assertThrows<IllegalArgumentException> { Quantity.positive(0L) }
        assertThrows<IllegalArgumentException> { Quantity.positive(-5L) }
        assertEquals(5L, Quantity.positive(5L).value)
    }

    @Test
    fun `덧셈과 뺄셈이 동작한다`() {
        assertEquals(Quantity.ofInt(15), Quantity.ofInt(10) + Quantity.ofInt(5))
        assertEquals(Quantity.ofInt(5), Quantity.ofInt(10).minus(Quantity.ofInt(5)))
    }

    @Test
    fun `보유보다 큰 수량 차감은 예외`() {
        assertThrows<IllegalArgumentException> { Quantity.ofInt(5).minus(Quantity.ofInt(10)) }
    }

    @Test
    fun `isZero 가 동작한다`() {
        assertTrue(Quantity.ZERO.isZero)
    }
}
