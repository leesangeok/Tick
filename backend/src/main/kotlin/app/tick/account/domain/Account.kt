package app.tick.account.domain

import app.tick.common.domain.Money
import app.tick.common.exception.InsufficientBalanceException
import java.time.Instant

class Account(
    val id: Long,
    val externalId: String,
    val memberId: Long?,
    cash: Money,
    totalDeposits: Money,
    val createdAt: Instant,
    updatedAt: Instant,
) {
    var cash: Money = cash
        private set
    var totalDeposits: Money = totalDeposits
        private set
    var updatedAt: Instant = updatedAt
        private set

    fun deposit(amount: Money, at: Instant = Instant.now()) {
        require(amount.value > 0) { "충전 금액은 0보다 커야 합니다." }
        cash += amount
        totalDeposits += amount
        updatedAt = at
    }

    fun withdraw(amount: Money, at: Instant = Instant.now()) {
        if (cash.isLessThan(amount)) {
            throw InsufficientBalanceException(amount.value, cash.value)
        }
        cash = cash.minus(amount)
        updatedAt = at
    }

    fun credit(amount: Money, at: Instant = Instant.now()) {
        cash += amount
        updatedAt = at
    }

    companion object {
        fun newForMember(externalId: String, memberId: Long, welcomeBonus: Money): Account {
            val now = Instant.now()
            return Account(
                id = 0L,
                externalId = externalId,
                memberId = memberId,
                cash = welcomeBonus,
                totalDeposits = welcomeBonus,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
