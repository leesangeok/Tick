package app.tick.account

import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByExternalId(externalId: String): Account?
    fun findByMemberId(memberId: Long): Account?
}

interface HoldingRepository : JpaRepository<Holding, Long> {
    fun findAllByAccountId(accountId: Long): List<Holding>
}

interface OrderHistoryRepository : JpaRepository<OrderHistory, Long> {
    fun findAllByAccountIdOrderByCreatedAtDesc(accountId: Long): List<OrderHistory>
}

interface DepositHistoryRepository : JpaRepository<DepositHistory, Long> {
    fun findAllByAccountIdOrderByCreatedAtDesc(accountId: Long): List<DepositHistory>
}
