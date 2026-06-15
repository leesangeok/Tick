package app.tick.auth

import app.tick.common.exception.BusinessException
import app.tick.common.exception.ErrorCode
import app.tick.common.response.ApiResponse
import app.tick.member.MemberRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class MeResponse(
    val id: Long,
    val nickname: String?,
    val email: String?,
)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val memberRepository: MemberRepository,
    private val jwtCookies: JwtCookies,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val refreshTokenService: RefreshTokenService,
) {
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: AuthPrincipal?): ApiResponse<MeResponse> {
        if (principal == null) throw BusinessException(ErrorCode.UNAUTHORIZED)
        val member = memberRepository.findById(principal.memberId).orElseThrow {
            BusinessException(ErrorCode.UNAUTHORIZED)
        }
        return ApiResponse.success(
            MeResponse(id = member.id, nickname = member.nickname, email = member.email),
        )
    }

    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): ApiResponse<Unit> {
        val raw = JwtCookies.readRefresh(request) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val memberId = refreshTokenService.validate(raw) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val member = memberRepository.findById(memberId).orElseThrow {
            BusinessException(ErrorCode.UNAUTHORIZED)
        }
        val newAccess = jwtProvider.issueAccessToken(member.id, member.nickname)
        jwtCookies.writeAccess(response, newAccess, (jwtProperties.accessTtlMin * 60).toInt())
        return ApiResponse.success()
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ApiResponse<Unit> {
        JwtCookies.readRefresh(request)?.let { refreshTokenService.revoke(it) }
        jwtCookies.clear(response)
        return ApiResponse.success()
    }
}
