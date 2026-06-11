package app.tick.common.domain

@JvmInline
value class Money(val value: Long) : Comparable<Money> {
    init {
        require(value >= 0) { "금액은 0 이상이어야 합니다: $value" }
    }

    operator fun plus(other: Money): Money = Money(value + other.value)

    fun minus(other: Money): Money {
        require(value >= other.value) {
            "차감 금액이 현재 금액보다 큽니다. current=$value, minus=${other.value}"
        }
        return Money(value - other.value)
    }

    fun multiply(quantity: Quantity): Money = Money(value * quantity.value)

    override fun compareTo(other: Money): Int = value.compareTo(other.value)

    fun isLessThan(other: Money): Boolean = value < other.value

    companion object {
        val ZERO = Money(0L)

        fun of(value: Long): Money = Money(value)
        fun ofInt(value: Int): Money = Money(value.toLong())
    }
}
