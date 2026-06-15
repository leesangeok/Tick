package app.tick.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64

// backend-a / backend-b 라운드로빈 환경에서 인증 시작과 콜백이 서로 다른 인스턴스에
// 도착해도 동작하도록 OAuth2AuthorizationRequest 를 서버 세션 대신 쿠키에 저장한다.
@Component
class HttpCookieOAuth2AuthorizationRequestRepository :
    AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val value = readCookie(request) ?: return null
        return deserialize(value)
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (authorizationRequest == null) {
            writeCookie(response, "", 0)
            return
        }
        writeCookie(response, serialize(authorizationRequest), COOKIE_MAX_AGE_SECONDS)
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? {
        val loaded = loadAuthorizationRequest(request)
        writeCookie(response, "", 0)
        return loaded
    }

    private fun readCookie(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == COOKIE_NAME }?.value

    private fun writeCookie(response: HttpServletResponse, value: String, maxAge: Long) {
        val cookie = ResponseCookie.from(COOKIE_NAME, value)
            .path("/")
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .maxAge(maxAge)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun serialize(req: OAuth2AuthorizationRequest): String {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(req) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray())
    }

    private fun deserialize(value: String): OAuth2AuthorizationRequest? = try {
        val bytes = Base64.getUrlDecoder().decode(value)
        ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as OAuth2AuthorizationRequest
        }
    } catch (e: Exception) {
        log.warn("failed to deserialize oauth2 authorization request cookie: {}", e.message)
        null
    }

    companion object {
        private const val COOKIE_NAME = "OAUTH2_AUTH_REQUEST"
        private const val COOKIE_MAX_AGE_SECONDS = 180L
    }
}
