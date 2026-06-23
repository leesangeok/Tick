package app.tick.auth

import app.tick.common.response.ApiResponse
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class DevTokenRequest(
    val kakaoId: Long? = null,
    val nickname: String? = null,
    val email: String? = null,
)

data class DevTokenResponse(
    val memberId: Long,
    val nickname: String?,
    val accessToken: String,
    val refreshToken: String,
)

/**
 * dev/internal 전용 토큰 발급. 카카오 OAuth 통과 없이 Member + Account 를 upsert 하고
 * JWT access/refresh 를 즉시 발급한다. 응답 body 에 토큰을 노출하므로 cookie 가 안 잡히는
 * 환경 (curl, IDE HTTP client, integration test) 에서도 Bearer 헤더로 바로 호출 가능.
 *
 * 활성화: `tick.auth.dev-token-enabled=true` (compose.full.yaml 에서 활성, 기본 false).
 * prod 환경에선 반드시 false 유지.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(prefix = "tick.auth", name = ["dev-token-enabled"], havingValue = "true")
class DevAuthController(
    private val memberProvisioner: MemberProvisioner,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val refreshTokenService: RefreshTokenService,
    private val jwtCookies: JwtCookies,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/dev-token")
    fun devToken(
        @RequestBody(required = false) request: DevTokenRequest?,
        response: HttpServletResponse,
    ): ApiResponse<DevTokenResponse> {
        val req = request ?: DevTokenRequest()
        val kakaoId = req.kakaoId ?: DEFAULT_DEV_KAKAO_ID
        val member = memberProvisioner.upsertWithAccount(
            kakaoId = kakaoId,
            email = req.email ?: "dev-${kakaoId}@tick.local",
            nickname = req.nickname ?: "dev-${kakaoId}",
        )

        val access = jwtProvider.issueAccessToken(member.id, member.nickname)
        val refresh = refreshTokenService.issue(member.id)

        // 같은 cookie 도 굽는다 → 브라우저에서 바로 인증된 상태로 호출 가능.
        jwtCookies.writeAccess(response, access, (jwtProperties.accessTtlMin * 60).toInt())
        jwtCookies.writeRefresh(response, refresh, (jwtProperties.refreshTtlDays * 24 * 60 * 60).toInt())

        log.info("dev-token issued memberId={} kakaoId={}", member.id, kakaoId)

        return ApiResponse.success(
            DevTokenResponse(
                memberId = member.id,
                nickname = member.nickname,
                accessToken = access,
                refreshToken = refresh,
            ),
        )
    }

    companion object {
        // 기본 dev member 의 kakaoId (실제 카카오 user id 와 충돌 가능성 매우 낮음).
        const val DEFAULT_DEV_KAKAO_ID: Long = 0L
    }
}
