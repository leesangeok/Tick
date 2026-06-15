package app.tick.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val provisioner: MemberProvisioner,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val jwtCookies: JwtCookies,
    private val authProperties: AuthProperties,
    private val refreshTokenService: RefreshTokenService,
) : SimpleUrlAuthenticationSuccessHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauthUser = authentication.principal as OAuth2User
        val kakaoId = (oauthUser.attributes["kakaoId"] as Number).toLong()
        val email = oauthUser.attributes["email"] as String?
        val nickname = oauthUser.attributes["nickname"] as String?

        val member = provisioner.upsertWithAccount(kakaoId, email, nickname)
        val accessToken = jwtProvider.issueAccessToken(member.id, member.nickname)
        val refreshToken = refreshTokenService.issue(member.id)

        jwtCookies.writeAccess(response, accessToken, (jwtProperties.accessTtlMin * 60).toInt())
        jwtCookies.writeRefresh(response, refreshToken, (jwtProperties.refreshTtlDays * 86400).toInt())

        log.info("kakao login success kakaoId={} memberId={}", kakaoId, member.id)
        redirectStrategy.sendRedirect(request, response, authProperties.frontendCallbackUrl)
    }
}
