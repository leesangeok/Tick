package app.tick.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Entity
@Table(name = "refresh_token")
class RefreshTokenJpa(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
)

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshTokenJpa, Long> {
    fun findByTokenHash(tokenHash: String): RefreshTokenJpa?

    @Modifying
    @Query("UPDATE RefreshTokenJpa t SET t.revokedAt = :now WHERE t.memberId = :memberId AND t.revokedAt IS NULL")
    fun revokeAllForMember(@Param("memberId") memberId: Long, @Param("now") now: Instant): Int
}

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
