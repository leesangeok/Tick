package app.tick.account.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface AccountJpaRepository : JpaRepository<AccountJpaEntity, Long> {
    fun findByExternalId(externalId: String): AccountJpaEntity?
    fun findByMemberId(memberId: Long): AccountJpaEntity?
}

interface HoldingJpaRepository : JpaRepository<HoldingJpaEntity, Long> {
    fun findAllByAccountId(accountId: Long): List<HoldingJpaEntity>
    fun findByAccountIdAndSymbol(accountId: Long, symbol: String): HoldingJpaEntity?
}

interface DepositHistoryJpaRepository : JpaRepository<DepositHistoryJpaEntity, Long> {
    fun findAllByAccountIdOrderByCreatedAtDesc(accountId: Long): List<DepositHistoryJpaEntity>
}
