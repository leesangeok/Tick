package app.tick.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date

@Component
class JwtProvider(private val properties: JwtProperties) {
    private val key = Keys.hmacShaKeyFor(properties.secret.toByteArray(Charsets.UTF_8))

    fun issueAccessToken(memberId: Long, nickname: String?): String {
        val now = Instant.now()
        val expiry = now.plusSeconds(properties.accessTtlMin * 60)
        return Jwts.builder()
            .subject(memberId.toString())
            .claim("nickname", nickname)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact()
    }

    fun parse(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

    fun memberId(token: String): Long = parse(token).subject.toLong()
}
