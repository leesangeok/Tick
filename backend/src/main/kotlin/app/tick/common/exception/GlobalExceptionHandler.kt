package app.tick.common.exception

import app.tick.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.info("business exception code={} message={}", ex.errorCode.name, ex.message)
        return build(ex.errorCode, ex.message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        log.info("illegal argument: {}", ex.message)
        return build(ErrorCode.INVALID_REQUEST, ex.message ?: ErrorCode.INVALID_REQUEST.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.let {
            "${it.field}: ${it.defaultMessage}"
        } ?: ErrorCode.INVALID_REQUEST.message
        return build(ErrorCode.INVALID_REQUEST, message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        log.info("unreadable request body: {}", ex.message)
        return build(ErrorCode.INVALID_REQUEST, "요청 본문을 해석할 수 없습니다.")
    }

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("unhandled exception", ex)
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message)
    }

    private fun build(code: ErrorCode, message: String): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(code.status).body(ApiResponse.fail(code.name, message))
}
