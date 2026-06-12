package app.tick.common.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AveragePriceCalculatorTest {
    @Test
    fun `매수 시 평균 매입 단가 가중평균 계산`() {
        val newAverage = AveragePriceCalculator.afterBuy(
            currentQuantity = Quantity.ofInt(10),
            currentAveragePrice = Money.of(1000L),
            buyQuantity = Quantity.ofInt(10),
            buyPrice = Money.of(2000L),
        )
        // (1000 * 10 + 2000 * 10) / 20 = 30000 / 20 = 1500
        assertEquals(Money.of(1500L), newAverage)
    }

    @Test
    fun `보유 0주에서 첫 매수 시 매수가가 평균가`() {
        val newAverage = AveragePriceCalculator.afterBuy(
            currentQuantity = Quantity.ZERO,
            currentAveragePrice = Money.ZERO,
            buyQuantity = Quantity.ofInt(5),
            buyPrice = Money.of(1234L),
        )
        assertEquals(Money.of(1234L), newAverage)
    }

    @Test
    fun `실현손익은 매도가와 평균가의 차이 곱하기 매도수량`() {
        val realized = AveragePriceCalculator.realizedProfitLoss(
            sellPrice = Money.of(2000L),
            averagePrice = Money.of(1500L),
            sellQuantity = Quantity.ofInt(10),
        )
        // (2000 - 1500) * 10 = 5000
        assertEquals(ProfitLoss.of(5000L), realized)
    }

    @Test
    fun `매도가가 평균가보다 낮으면 실현손실`() {
        val realized = AveragePriceCalculator.realizedProfitLoss(
            sellPrice = Money.of(800L),
            averagePrice = Money.of(1000L),
            sellQuantity = Quantity.ofInt(5),
        )
        // (800 - 1000) * 5 = -1000
        assertEquals(ProfitLoss.of(-1000L), realized)
    }
}
