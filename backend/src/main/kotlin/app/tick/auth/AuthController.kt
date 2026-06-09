package app.tick.auth

import app.tick.member.MemberRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
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
@RequestMapping("/api/auth")
class AuthController(
    private val memberRepository: MemberRepository,
) {
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: AuthPrincipal?): ResponseEntity<MeResponse> {
        if (principal == null) return ResponseEntity.status(401).build()
        val member = memberRepository.findById(principal.memberId).orElse(null)
            ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(
            MeResponse(id = member.id, nickname = member.nickname, email = member.email),
        )
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        JwtCookies.clear(response)
        return ResponseEntity.noContent().build()
    }
}
