package app.tick.support

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * 통합 테스트용 직접 SQL seed. JPA 를 통하지 않고 부작용 없이 최소 데이터만 삽입.
 * 정리는 각 테스트 클래스가 필요 시 truncate 하는 방식 (지금은 테스트당 유니크한 member_id 사용).
 */
@Component
class TestDataSeeder(private val jdbc: JdbcTemplate) {

    fun insertMember(kakaoId: Long, nickname: String = "tester"): Long {
        val sql = """
            INSERT INTO member (kakao_id, email, nickname)
            VALUES (?, ?, ?)
            RETURNING id
        """
        return jdbc.queryForObject(sql, Long::class.java, kakaoId, "$kakaoId@test.local", nickname)!!
    }

    fun insertAccount(memberId: Long, cash: Long = 10_000_000L, totalDeposits: Long = 10_000_000L): Long {
        val sql = """
            INSERT INTO account (external_id, member_id, cash, total_deposits)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """
        return jdbc.queryForObject(
            sql, Long::class.java,
            "member-$memberId", memberId, cash, totalDeposits,
        )!!
    }
}
