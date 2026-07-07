package app.tick.common.ratelimit

import app.tick.auth.AuthPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

/**
 * Redis 고정 창(fixed window) 카운터 기반 rate limit.
 *
 * 키: `rl:{bucket}:{memberId}:{epochWindow}`
 * 알고리즘: INCR key → 1 이면 EXPIRE windowSec → count > limit 이면 429.
 *
 * Anonymous 요청 (JWT 없음) 은 skip — 인증 필요 엔드포인트는 SecurityFilter 가 먼저 401 로 막고,
 * public 엔드포인트는 GET 위주라 abuse 위험이 낮다. IP 기반 rate limit 은 다음 iteration.
 *
 * Redis 다운/장애 시 fail-open (allow). 서비스 전체가 429 로 잠기는 것보단 낫다.
 */
@Component
class RateLimitInterceptor(
    private val redisTemplate: StringRedisTemplate,
) : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        val memberId = currentMemberId() ?: return true

        val annotation = handler.getMethodAnnotation(RateLimited::class.java)
            ?: handler.beanType.getAnnotation(RateLimited::class.java)
        val bucket = annotation?.bucket ?: DEFAULT_BUCKET
        val limit = annotation?.limit ?: DEFAULT_LIMIT
        val windowSec = annotation?.windowSec ?: DEFAULT_WINDOW_SEC

        val nowSec = System.currentTimeMillis() / 1000
        val windowStart = (nowSec / windowSec) * windowSec
        val key = "rl:$bucket:$memberId:$windowStart"

        val count: Long = try {
            val incremented = redisTemplate.opsForValue().increment(key) ?: 1L
            if (incremented == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSec))
            }
            incremented
        } catch (e: Exception) {
            log.warn("redis unavailable, fail-open rate limit key={} err={}", key, e.message)
            return true
        }

        if (count > limit) {
            val retryAfter = windowSec - (nowSec - windowStart)
            response.setHeader("Retry-After", retryAfter.toString())
            throw RateLimitExceededException(bucket = bucket, retryAfterSec = retryAfter)
        }
        return true
    }

    private fun currentMemberId(): Long? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        val principal = auth.principal as? AuthPrincipal ?: return null
        return principal.memberId
    }

    companion object {
        private const val DEFAULT_BUCKET = "default"
        private const val DEFAULT_LIMIT = 60
        private const val DEFAULT_WINDOW_SEC = 60L
    }
}
