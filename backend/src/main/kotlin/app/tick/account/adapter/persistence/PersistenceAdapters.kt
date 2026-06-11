package app.tick.account.adapter.persistence

import app.tick.account.application.LoadAccountPort
import app.tick.account.application.LoadDepositHistoryPort
import app.tick.account.application.LoadHoldingPort
import app.tick.account.application.SaveAccountPort
import app.tick.account.application.SaveDepositPort
import app.tick.account.application.SaveHoldingPort
import app.tick.account.domain.Account
import app.tick.account.domain.Deposit
import app.tick.account.domain.Holding
import app.tick.common.domain.StockCode
import org.springframework.stereotype.Component

@Component
class AccountPersistenceAdapter(
    private val repository: AccountJpaRepository,
) : LoadAccountPort, SaveAccountPort {
    override fun loadByMemberId(memberId: Long): Account? =
        repository.findByMemberId(memberId)?.let(AccountMapper::toDomain)

    override fun loadByExternalId(externalId: String): Account? =
        repository.findByExternalId(externalId)?.let(AccountMapper::toDomain)

    override fun save(account: Account): Account {
        val saved = repository.save(AccountMapper.toEntity(account))
        return AccountMapper.toDomain(saved)
    }
}

@Component
class HoldingPersistenceAdapter(
    private val repository: HoldingJpaRepository,
) : LoadHoldingPort, SaveHoldingPort {
    override fun loadAllByAccountId(accountId: Long): List<Holding> =
        repository.findAllByAccountId(accountId).map(HoldingMapper::toDomain)

    override fun loadByAccountIdAndStockCode(accountId: Long, stockCode: StockCode): Holding? =
        repository.findByAccountIdAndSymbol(accountId, stockCode.value)?.let(HoldingMapper::toDomain)

    override fun save(holding: Holding): Holding {
        val saved = repository.save(HoldingMapper.toEntity(holding))
        return HoldingMapper.toDomain(saved)
    }
}

@Component
class DepositHistoryPersistenceAdapter(
    private val repository: DepositHistoryJpaRepository,
) : LoadDepositHistoryPort, SaveDepositPort {
    override fun loadAllByAccountIdDesc(accountId: Long): List<Deposit> =
        repository.findAllByAccountIdOrderByCreatedAtDesc(accountId).map(DepositMapper::toDomain)

    override fun save(deposit: Deposit): Deposit {
        val saved = repository.save(DepositMapper.toEntity(deposit))
        return DepositMapper.toDomain(saved)
    }
}
