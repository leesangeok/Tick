package app.tick.account

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val orderHistoryRepository: OrderHistoryRepository,
    private val depositHistoryRepository: DepositHistoryRepository,
) {
    @Transactional(readOnly = true)
    fun get(memberId: Long): AccountResponse {
        val account = findAccount(memberId)
        return toResponse(account)
    }

    @Transactional
    fun deposit(memberId: Long, amount: Long): AccountResponse {
        require(amount > 0) { "충전 금액은 0보다 커야 합니다." }
        val account = findAccount(memberId)
        account.cash += amount
        account.totalDeposits += amount
        account.updatedAt = Instant.now()
        accountRepository.save(account)
        depositHistoryRepository.save(
            DepositHistory(
                accountId = account.id,
                amount = amount,
                type = "DEPOSIT",
            ),
        )
        return toResponse(account)
    }

    private fun findAccount(memberId: Long): Account =
        accountRepository.findByMemberId(memberId)
            ?: error("Account not found for member: $memberId")

    private fun toResponse(account: Account): AccountResponse {
        val realized = orderHistoryRepository
            .findAllByAccountIdOrderByCreatedAtDesc(account.id)
            .sumOf { it.realizedProfitLoss ?: 0L }
        return AccountResponse(
            id = account.id,
            cash = account.cash,
            totalDeposits = account.totalDeposits,
            realizedProfitLoss = realized,
        )
    }
}
