package app.tick.account.domain

import app.tick.common.domain.Money
import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode
import app.tick.common.exception.InsufficientStockQuantityException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals

class HoldingTest {
    private val now = Instant.parse("2026-06-12T00:00:00Z")
    private val stockCode = StockCode.of("005930")

    private fun newHolding(quantity: Int = 10, averagePrice: Long = 1000L): Holding =
        Holding(
            id = 1L,
            accountId = 1L,
            stockCode = stockCode,
            quantity = Quantity.ofInt(quantity),
            averagePrice = Money.of(averagePrice),
            createdAt = now,
            updatedAt = now,
        )

    @Test
    fun `applyBuy 는 가중평균으로 평균가 재계산하고 수량 증가`() {
        val holding = newHolding(quantity = 10, averagePrice = 1000L)
        holding.applyBuy(Quantity.ofInt(10), Money.of(2000L))
        assertEquals(Quantity.ofInt(20), holding.quantity)
        // (1000 * 10 + 2000 * 10) / 20 = 1500
        assertEquals(Money.of(1500L), holding.averagePrice)
    }

    @Test
    fun `applySell 은 수량 차감`() {
        val holding = newHolding(quantity = 10, averagePrice = 1500L)
        holding.applySell(Quantity.ofInt(3))
        assertEquals(Quantity.ofInt(7), holding.quantity)
        // 평균가는 매도해도 안 바뀜
        assertEquals(Money.of(1500L), holding.averagePrice)
    }

    @Test
    fun `보유 수량보다 큰 매도는 InsufficientStockQuantityException`() {
        val holding = newHolding(quantity = 5)
        assertThrows<InsufficientStockQuantityException> { holding.applySell(Quantity.ofInt(10)) }
    }

    @Test
    fun `newFromBuy 팩토리는 매수가가 평균가가 된다`() {
        val holding = Holding.newFromBuy(
            accountId = 1L,
            stockCode = stockCode,
            buyQuantity = Quantity.ofInt(5),
            buyPrice = Money.of(3000L),
            at = now,
        )
        assertEquals(Quantity.ofInt(5), holding.quantity)
        assertEquals(Money.of(3000L), holding.averagePrice)
        assertEquals(stockCode, holding.stockCode)
    }

    @Test
    fun `수량 0 까지 매도하면 수량은 0 이고 평균가는 유지`() {
        val holding = newHolding(quantity = 5, averagePrice = 1000L)
        holding.applySell(Quantity.ofInt(5))
        assertEquals(Quantity.ZERO, holding.quantity)
        assertEquals(Money.of(1000L), holding.averagePrice)
    }
}
