package app.tick.common.ratelimit

import app.tick.common.exception.BusinessException
import app.tick.common.exception.ErrorCode

class RateLimitExceededException(
    val bucket: String,
    val retryAfterSec: Long,
) : BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "요청이 너무 많습니다. ${retryAfterSec}초 후 다시 시도해 주세요.")
