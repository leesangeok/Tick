package app.tick.common.domain

@JvmInline
value class Quantity(val value: Long) : Comparable<Quantity> {
    init {
        require(value >= 0) { "수량은 0 이상이어야 합니다: $value" }
    }

    operator fun plus(other: Quantity): Quantity = Quantity(value + other.value)

    fun minus(other: Quantity): Quantity {
        require(value >= other.value) {
            "차감 수량이 현재 수량보다 큽니다. current=$value, minus=${other.value}"
        }
        return Quantity(value - other.value)
    }

    override fun compareTo(other: Quantity): Int = value.compareTo(other.value)

    val isZero: Boolean get() = value == 0L

    val toInt: Int get() = value.toInt()

    companion object {
        val ZERO = Quantity(0L)

        fun of(value: Long): Quantity = Quantity(value)
        fun ofInt(value: Int): Quantity = Quantity(value.toLong())

        fun positive(value: Long): Quantity {
            require(value > 0) { "수량은 0보다 커야 합니다: $value" }
            return Quantity(value)
        }
    }
}
