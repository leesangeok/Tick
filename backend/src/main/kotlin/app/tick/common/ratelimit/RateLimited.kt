package app.tick.common.ratelimit

/**
 * 컨트롤러 메서드 (또는 클래스) 에 붙여 이 엔드포인트의 rate limit 을 조정.
 * 없으면 `RateLimitInterceptor` 의 기본값 (default 그룹, 60/분) 적용.
 *
 * bucket 이름 다르면 서로 독립. AI 요약 같은 고비용 엔드포인트는 별도 bucket 으로 뽑아
 * 한 유저가 대량 호출해도 다른 엔드포인트 quota 는 남는다.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimited(
    val bucket: String = "default",
    val limit: Int = 60,
    val windowSec: Long = 60,
)
