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
)
