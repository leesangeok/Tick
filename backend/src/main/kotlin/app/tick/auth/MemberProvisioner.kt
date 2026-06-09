package app.tick.auth

import app.tick.account.Account
import app.tick.account.AccountRepository
import app.tick.member.Member
import app.tick.member.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 카카오 로그인 성공 시 호출.
 * 1. kakao_id 로 기존 Member 조회 → 없으면 신규 생성
 * 2. 신규 Member 면 Account 도 함께 생성 (가상 시드머니 지급)
 * 3. Account 가 없는 기존 Member 면 Account 생성
 */
@Service
class MemberProvisioner(
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository,
) {
    companion object {
        const val WELCOME_BONUS: Long = 10_000_000
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

        if (accountRepository.findByMemberId(member.id) == null) {
            accountRepository.save(
                Account(
                    externalId = "kakao_${kakaoId}_${UUID.randomUUID().toString().take(8)}",
                    memberId = member.id,
                    cash = WELCOME_BONUS,
                    totalDeposits = WELCOME_BONUS,
                ),
            )
        }

        return member
    }
}
