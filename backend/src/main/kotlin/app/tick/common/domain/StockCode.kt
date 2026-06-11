package app.tick.common.domain

@JvmInline
value class StockCode(val value: String) {
    init {
        require(value.isNotBlank()) { "종목코드는 빈 문자열일 수 없습니다." }
        require(value.length in 5..10) { "종목코드 길이가 올바르지 않습니다: $value" }
    }

    override fun toString(): String = value

    companion object {
        fun of(value: String): StockCode = StockCode(value)
    }
}
