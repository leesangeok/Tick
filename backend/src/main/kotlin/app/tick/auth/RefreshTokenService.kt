package app.tick.auth

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Refresh token 발급 / 검증 / 폐기. Redis 백엔드.
 *
 * 저장 구조:
 * - `auth:refresh:{hash}` → memberId (String), TTL = `refreshTtlDays`
 *   토큰 자체는 raw 가 아니라 SHA-256 hash 만 저장. 사용자 토큰이 유출돼도 server-side DB
 *   에는 raw 가 없음.
 * - `auth:refresh:by-member:{memberId}` → Set<hash>, TTL = `refreshTtlDays`
 *   한 멤버가 발급받은 모든 hash 의 inverted index. `revokeAllForMember` 용.
 *
 * Redis TTL 자체가 만료 처리 → 별도 cleanup cron 불필요. revoke 는 단순 DELETE.
 * 이전 구현 (Postgres `refresh_token` 테이블 + JPA) 는 폐기. 테이블 자체는 다음 마이그
 * 레이션에서 drop 예정 (별도 PR).
 */
@Service
class RefreshTokenService(
    private val redisTemplate: StringRedisTemplate,
    private val properties: JwtProperties,
) {
    fun issue(memberId: Long): String {
        val raw = RefreshTokenCrypto.generate()
        val hash = RefreshTokenCrypto.hash(raw)
        val ttl = Duration.ofDays(properties.refreshTtlDays)

        redisTemplate.opsForValue().set(tokenKey(hash), memberId.toString(), ttl)
        redisTemplate.opsForSet().add(memberIndexKey(memberId), hash)
        redisTemplate.expire(memberIndexKey(memberId), ttl)

        return raw
    }

    fun validate(raw: String): Long? {
        val hash = RefreshTokenCrypto.hash(raw)
        return redisTemplate.opsForValue().get(tokenKey(hash))?.toLongOrNull()
    }

    fun revoke(raw: String) {
        val hash = RefreshTokenCrypto.hash(raw)
        val memberId = redisTemplate.opsForValue().get(tokenKey(hash))?.toLongOrNull() ?: return
        redisTemplate.delete(tokenKey(hash))
        redisTemplate.opsForSet().remove(memberIndexKey(memberId), hash)
    }

    fun revokeAllForMember(memberId: Long) {
        val hashes = redisTemplate.opsForSet().members(memberIndexKey(memberId)) ?: return
        if (hashes.isNotEmpty()) {
            redisTemplate.delete(hashes.map { tokenKey(it) })
        }
        redisTemplate.delete(memberIndexKey(memberId))
    }

    private fun tokenKey(hash: String) = "$TOKEN_KEY_PREFIX$hash"
    private fun memberIndexKey(memberId: Long) = "$MEMBER_INDEX_KEY_PREFIX$memberId"

    companion object {
        private const val TOKEN_KEY_PREFIX = "auth:refresh:"
        private const val MEMBER_INDEX_KEY_PREFIX = "auth:refresh:by-member:"
    }
}
