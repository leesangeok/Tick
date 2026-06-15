package app.tick.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tick.jwt")
data class JwtProperties(
    val secret: String,
    val accessTtlMin: Long = 60,
    val refreshTtlDays: Long = 14,
)

@ConfigurationProperties(prefix = "tick.auth")
data class AuthProperties(
    val frontendCallbackUrl: String = "http://localhost:3000/auth/callback",
    val frontendLoginUrl: String = "http://localhost:3000/login",
    val cookieSameSite: String = "Lax",
    val cookieSecure: Boolean = false,
    // 빈 문자열/null 이면 Domain 속성 미부여 (set 한 호스트에만 유효).
    // 프론트와 백엔드가 같은 root 도메인의 서로 다른 서브도메인일 때 ".tickk.dev" 같은 값을 주면
    // 두 서브도메인이 같은 쿠키를 공유한다.
    val cookieDomain: String? = null,
)
