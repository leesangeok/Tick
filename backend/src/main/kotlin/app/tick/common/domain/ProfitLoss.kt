package app.tick.common.domain

@JvmInline
value class ProfitLoss(val value: Long) {
    operator fun plus(other: ProfitLoss): ProfitLoss = ProfitLoss(value + other.value)
    operator fun minus(other: ProfitLoss): ProfitLoss = ProfitLoss(value - other.value)
    operator fun unaryMinus(): ProfitLoss = ProfitLoss(-value)

    val isPositive: Boolean get() = value > 0
    val isNegative: Boolean get() = value < 0
    val isZero: Boolean get() = value == 0L

    companion object {
        val ZERO = ProfitLoss(0L)

        fun of(value: Long): ProfitLoss = ProfitLoss(value)

        fun between(after: Money, before: Money): ProfitLoss =
            ProfitLoss(after.value - before.value)
    }
}
