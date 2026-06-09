package app.tick.auth

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)
        if (token != null) {
            try {
                val claims = jwtProvider.parse(token)
                val principal = AuthPrincipal(
                    memberId = claims.subject.toLong(),
                    nickname = claims["nickname"] as String?,
                )
                val auth = UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.authorities,
                )
                SecurityContextHolder.getContext().authentication = auth
            } catch (e: JwtException) {
                log.debug("Invalid JWT: {}", e.message)
            } catch (e: IllegalArgumentException) {
                log.debug("Malformed JWT: {}", e.message)
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) return header.substring(7)
        return JwtCookies.read(request)
    }
}
