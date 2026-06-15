package app.tick.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

const val ACCESS_TOKEN_COOKIE = "tick_at"
const val REFRESH_TOKEN_COOKIE = "tick_rt"

@Component
class JwtCookies(private val authProperties: AuthProperties) {
    fun writeAccess(response: HttpServletResponse, token: String, maxAgeSec: Int) {
        response.addHeader("Set-Cookie", buildCookie(ACCESS_TOKEN_COOKIE, token, maxAgeSec))
    }

    fun writeRefresh(response: HttpServletResponse, token: String, maxAgeSec: Int) {
        response.addHeader("Set-Cookie", buildCookie(REFRESH_TOKEN_COOKIE, token, maxAgeSec))
    }

    fun clear(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", buildCookie(ACCESS_TOKEN_COOKIE, "", 0))
        response.addHeader("Set-Cookie", buildCookie(REFRESH_TOKEN_COOKIE, "", 0))
    }

    private fun buildCookie(name: String, value: String, maxAgeSec: Int): String {
        val parts = mutableListOf(
            "$name=$value",
            "HttpOnly",
            "Path=/",
            "Max-Age=$maxAgeSec",
            "SameSite=${authProperties.cookieSameSite}",
        )
        if (authProperties.cookieSecure) parts += "Secure"
        authProperties.cookieDomain?.takeIf { it.isNotBlank() }?.let { parts += "Domain=$it" }
        return parts.joinToString("; ")
    }

    companion object {
        fun readAccess(request: HttpServletRequest): String? =
            request.cookies?.firstOrNull { it.name == ACCESS_TOKEN_COOKIE }?.value

        fun readRefresh(request: HttpServletRequest): String? =
            request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE }?.value
    }
}
