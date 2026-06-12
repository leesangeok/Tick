package app.tick.account.domain

import app.tick.common.domain.Money
import app.tick.common.exception.InsufficientBalanceException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals

class AccountTest {
    private fun newAccount(cash: Long = 1_000_000L, totalDeposits: Long = 1_000_000L): Account {
        val now = Instant.parse("2026-06-12T00:00:00Z")
        return Account(
            id = 1L,
            externalId = "test",
            memberId = 1L,
            cash = Money.of(cash),
            totalDeposits = Money.of(totalDeposits),
            createdAt = now,
            updatedAt = now,
        )
    }

    @Test
    fun `입금하면 cash 와 totalDeposits 가 동시에 증가한다`() {
        val account = newAccount(cash = 1_000_000L, totalDeposits = 1_000_000L)
        account.deposit(Money.of(500_000L))
        assertEquals(Money.of(1_500_000L), account.cash)
        assertEquals(Money.of(1_500_000L), account.totalDeposits)
    }

    @Test
    fun `0원 입금은 거부`() {
        val account = newAccount()
        assertThrows<IllegalArgumentException> { account.deposit(Money.ZERO) }
    }

    @Test
    fun `출금하면 cash 만 감소하고 totalDeposits 는 그대로`() {
        val account = newAccount(cash = 1_000_000L, totalDeposits = 1_000_000L)
        account.withdraw(Money.of(300_000L))
        assertEquals(Money.of(700_000L), account.cash)
        assertEquals(Money.of(1_000_000L), account.totalDeposits)
    }

    @Test
    fun `잔고 부족 시 출금은 InsufficientBalanceException`() {
        val account = newAccount(cash = 100_000L)
        assertThrows<InsufficientBalanceException> { account.withdraw(Money.of(200_000L)) }
    }

    @Test
    fun `credit 은 매도 대금처럼 cash 만 증가시키고 totalDeposits 는 그대로`() {
        val account = newAccount(cash = 500_000L, totalDeposits = 1_000_000L)
        account.credit(Money.of(200_000L))
        assertEquals(Money.of(700_000L), account.cash)
        assertEquals(Money.of(1_000_000L), account.totalDeposits)
    }

    @Test
    fun `상태 변경 시 updatedAt 이 갱신된다`() {
        val account = newAccount()
        val before = account.updatedAt
        val later = Instant.parse("2026-06-12T01:00:00Z")
        account.deposit(Money.of(100L), later)
        assertEquals(later, account.updatedAt)
        assert(later > before)
    }

    @Test
    fun `newForMember 팩토리는 환영 보너스를 cash 와 totalDeposits 양쪽에 적용`() {
        val account = Account.newForMember("ext_1", memberId = 42L, welcomeBonus = Money.of(10_000_000L))
        assertEquals(Money.of(10_000_000L), account.cash)
        assertEquals(Money.of(10_000_000L), account.totalDeposits)
        assertEquals(42L, account.memberId)
        assertEquals("ext_1", account.externalId)
    }
}
