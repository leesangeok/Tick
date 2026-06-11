package app.tick.account.domain

import app.tick.common.domain.Money
import java.time.Instant

enum class DepositType { DEPOSIT, WITHDRAW }

class Deposit(
    val id: Long,
    val accountId: Long,
    val amount: Money,
    val type: DepositType,
    val createdAt: Instant,
) {
    companion object {
        fun deposit(accountId: Long, amount: Money, at: Instant = Instant.now()): Deposit =
            Deposit(0L, accountId, amount, DepositType.DEPOSIT, at)
    }
}
