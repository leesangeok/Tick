package app.tick.stock

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PricePoint(
    val date: LocalDate,
    val open: Int,
    val high: Int,
    val low: Int,
    val close: Int,
    val volume: Long,
)

/**
 * 가격 시계열 생성기. 프론트엔드의 mocks/priceSeries.ts (mulberry32 + djb2-like hash)와
 * 동일한 알고리즘으로 구현되어 두 쪽이 같은 값을 산출함.
 */
@Component
class StockPriceGenerator {
    private val seoul: ZoneId = ZoneId.of("Asia/Seoul")

    fun generate(symbol: String, basePrice: Int, days: Int = 60): List<PricePoint> {
        val rand = mulberry32(hashString(symbol))
        val points = ArrayList<PricePoint>(days)
        var price = basePrice.toDouble()
        val today = LocalDate.now(seoul)
        val basePriceDouble = basePrice.toDouble()

        for (i in (days - 1) downTo 0) {
            val date = today.minusDays(i.toLong())
            val drift = (rand() - 0.48) * 0.05
            val open = price
            val close = max(price * (1 + drift), basePriceDouble * 0.5)
            val high = max(open, close) * (1 + rand() * 0.012)
            val low = min(open, close) * (1 - rand() * 0.012)
            val volume = floor(rand() * 5_000_000 + 800_000).toLong()

            points.add(
                PricePoint(
                    date = date,
                    open = open.roundToInt(),
                    high = high.roundToInt(),
                    low = low.roundToInt(),
                    close = close.roundToInt(),
                    volume = volume,
                ),
            )
            price = close
        }
        return points
    }

    private fun mulberry32(seed: Int): () -> Double {
        var state = seed
        return {
            state += 0x6d2b79f5
            var t = (state xor (state ushr 15)) * (1 or state)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            (t xor (t ushr 14)).toLong().and(0xFFFFFFFFL).toDouble() / 4294967296.0
        }
    }

    private fun hashString(str: String): Int {
        var hash = 0
        for (c in str) {
            hash = (hash shl 5) - hash + c.code
        }
        return hash
    }
}
