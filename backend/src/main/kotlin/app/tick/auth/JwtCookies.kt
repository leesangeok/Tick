package app.tick.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

const val ACCESS_TOKEN_COOKIE = "tick_at"

@Component
class JwtCookies(private val authProperties: AuthProperties) {
    fun write(response: HttpServletResponse, token: String, maxAgeSec: Int) {
        response.addHeader("Set-Cookie", buildCookie(token, maxAgeSec))
    }

    fun clear(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", buildCookie("", 0))
    }

    private fun buildCookie(value: String, maxAgeSec: Int): String {
        val parts = mutableListOf(
            "$ACCESS_TOKEN_COOKIE=$value",
            "HttpOnly",
            "Path=/",
            "Max-Age=$maxAgeSec",
            "SameSite=${authProperties.cookieSameSite}",
        )
        if (authProperties.cookieSecure) parts += "Secure"
        return parts.joinToString("; ")
    }

    companion object {
        fun read(request: HttpServletRequest): String? =
            request.cookies?.firstOrNull { it.name == ACCESS_TOKEN_COOKIE }?.value
    }
}
