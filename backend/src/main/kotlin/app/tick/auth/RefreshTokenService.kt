package app.tick.auth

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class RefreshTokenService(
    private val repository: RefreshTokenRepository,
    private val properties: JwtProperties,
) {
    @Transactional
    fun issue(memberId: Long): String {
        val raw = RefreshTokenCrypto.generate()
        val hash = RefreshTokenCrypto.hash(raw)
        val expiresAt = Instant.now().plus(properties.refreshTtlDays, ChronoUnit.DAYS)
        repository.save(RefreshTokenJpa(memberId = memberId, tokenHash = hash, expiresAt = expiresAt))
        return raw
    }

    @Transactional(readOnly = true)
    fun validate(raw: String): Long? {
        val token = repository.findByTokenHash(RefreshTokenCrypto.hash(raw)) ?: return null
        if (token.revokedAt != null) return null
        if (token.expiresAt.isBefore(Instant.now())) return null
        return token.memberId
    }

    @Transactional
    fun revoke(raw: String) {
        val token = repository.findByTokenHash(RefreshTokenCrypto.hash(raw)) ?: return
        if (token.revokedAt == null) {
            token.revokedAt = Instant.now()
            repository.save(token)
        }
    }

    @Transactional
    fun revokeAllForMember(memberId: Long) {
        repository.revokeAllForMember(memberId, Instant.now())
    }
}
