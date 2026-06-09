package app.tick.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

object JwtCookies {
    const val ACCESS_TOKEN_COOKIE = "tick_at"

    fun write(response: HttpServletResponse, token: String, maxAgeSec: Int) {
        // SameSite 직접 제어를 위해 Set-Cookie 헤더 수동 작성.
        // HTTPS 운영 환경에선 "; Secure" 추가.
        response.addHeader(
            "Set-Cookie",
            "$ACCESS_TOKEN_COOKIE=$token; HttpOnly; Path=/; Max-Age=$maxAgeSec; SameSite=Lax",
        )
    }

    fun clear(response: HttpServletResponse) {
        response.addHeader(
            "Set-Cookie",
            "$ACCESS_TOKEN_COOKIE=; HttpOnly; Path=/; Max-Age=0; SameSite=Lax",
        )
    }

    fun read(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == ACCESS_TOKEN_COOKIE }?.value
}
