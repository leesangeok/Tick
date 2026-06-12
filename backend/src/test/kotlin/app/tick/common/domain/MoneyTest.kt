package app.tick.common.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun `0 이상의 값으로 생성된다`() {
        assertEquals(0L, Money.ZERO.value)
        assertEquals(1000L, Money.of(1000L).value)
        assertEquals(500L, Money.ofInt(500).value)
    }

    @Test
    fun `음수로 생성하면 예외`() {
        assertThrows<IllegalArgumentException> { Money.of(-1L) }
    }

    @Test
    fun `더하기는 두 금액의 합`() {
        assertEquals(Money.of(300L), Money.of(100L) + Money.of(200L))
    }

    @Test
    fun `빼기는 차감 결과를 반환한다`() {
        assertEquals(Money.of(100L), Money.of(300L).minus(Money.of(200L)))
    }

    @Test
    fun `차감 금액이 더 크면 예외`() {
        assertThrows<IllegalArgumentException> { Money.of(100L).minus(Money.of(200L)) }
    }

    @Test
    fun `multiply 는 단가 × 수량`() {
        assertEquals(Money.of(10_000L), Money.of(1000L).multiply(Quantity.ofInt(10)))
    }

    @Test
    fun `isLessThan 비교가 동작한다`() {
        assertTrue(Money.of(100L).isLessThan(Money.of(200L)))
        assertFalse(Money.of(200L).isLessThan(Money.of(200L)))
        assertFalse(Money.of(300L).isLessThan(Money.of(200L)))
    }

    @Test
    fun `compareTo 로 정렬 가능`() {
        val sorted = listOf(Money.of(300L), Money.of(100L), Money.of(200L)).sorted()
        assertEquals(listOf(Money.of(100L), Money.of(200L), Money.of(300L)), sorted)
    }
}
