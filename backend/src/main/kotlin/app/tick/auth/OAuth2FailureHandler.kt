package app.tick.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OAuth2FailureHandler(
    private val authProperties: AuthProperties,
) : SimpleUrlAuthenticationFailureHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        log.warn("oauth2 login failure: {}", exception.message)
        val encoded = URLEncoder.encode(exception.message ?: "oauth_failure", StandardCharsets.UTF_8)
        redirectStrategy.sendRedirect(
            request,
            response,
            "${authProperties.frontendLoginUrl}?error=$encoded",
        )
    }
}
