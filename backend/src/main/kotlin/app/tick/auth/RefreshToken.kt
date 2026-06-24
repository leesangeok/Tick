package app.tick.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Refresh token 생성 + hash 유틸. raw 토큰은 사용자 cookie 로만 보관, server-side 에는
 * SHA-256 hex hash 만 저장 (현재 Redis - [RefreshTokenService]).
 *
 * 이전엔 같은 파일에 JPA Entity (`RefreshTokenJpa`) + `RefreshTokenRepository` 도 있었으나
 * Redis 이전으로 폐기. legacy `refresh_token` 테이블 자체는 다음 마이그레이션에서 drop 예정.
 */
object RefreshTokenCrypto {
    private val rng = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(32)
        rng.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
