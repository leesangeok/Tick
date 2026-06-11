package app.tick.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val code: String?,
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(true, data, null, null)
        fun success(): ApiResponse<Unit> = ApiResponse(true, Unit, null, null)
        fun fail(code: String, message: String): ApiResponse<Nothing> =
            ApiResponse(false, null, message, code)
    }
}
