package app.tick.auth

import app.tick.account.application.ProvisionAccountUseCase
import app.tick.common.domain.Money
import app.tick.member.Member
import app.tick.member.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class MemberProvisioner(
    private val memberRepository: MemberRepository,
    private val provisionAccount: ProvisionAccountUseCase,
) {
    companion object {
        val WELCOME_BONUS: Money = Money.of(10_000_000)
    }

    @Transactional
    fun upsertWithAccount(kakaoId: Long, email: String?, nickname: String?): Member {
        val member = memberRepository.findByKakaoId(kakaoId)?.also {
            it.email = email ?: it.email
            it.nickname = nickname ?: it.nickname
            it.updatedAt = Instant.now()
            memberRepository.save(it)
        } ?: memberRepository.save(
            Member(kakaoId = kakaoId, email = email, nickname = nickname),
        )

        val externalId = "kakao_${kakaoId}_${UUID.randomUUID().toString().take(8)}"
        provisionAccount.ensureFor(member.id, externalId, WELCOME_BONUS)
        return member
    }
}
