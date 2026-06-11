package app.tick.account.application

import app.tick.account.domain.Account
import app.tick.account.domain.Deposit
import app.tick.common.domain.Money
import app.tick.common.exception.AccountNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AccountService(
    private val loadAccountPort: LoadAccountPort,
    private val saveAccountPort: SaveAccountPort,
    private val saveDepositPort: SaveDepositPort,
    private val loadOrderSummaryPort: LoadOrderSummaryPort,
) : GetAccountUseCase, DepositUseCase, ProvisionAccountUseCase {

    @Transactional(readOnly = true)
    override fun get(memberId: Long): AccountResult {
        val account = loadAccount(memberId)
        return toResult(account)
    }

    @Transactional
    override fun deposit(command: DepositCommand): AccountResult {
        val account = loadAccount(command.memberId)
        val now = Instant.now()
        account.deposit(command.amount, now)
        val saved = saveAccountPort.save(account)
        saveDepositPort.save(Deposit.deposit(saved.id, command.amount, now))
        return toResult(saved)
    }

    @Transactional
    override fun ensureFor(memberId: Long, externalId: String, welcomeBonus: Money): Account {
        loadAccountPort.loadByMemberId(memberId)?.let { return it }
        val account = Account.newForMember(externalId, memberId, welcomeBonus)
        return saveAccountPort.save(account)
    }

    private fun loadAccount(memberId: Long): Account =
        loadAccountPort.loadByMemberId(memberId) ?: throw AccountNotFoundException(memberId)

    private fun toResult(account: Account): AccountResult =
        AccountResult(
            id = account.id,
            cash = account.cash.value,
            totalDeposits = account.totalDeposits.value,
            realizedProfitLoss = loadOrderSummaryPort.realizedProfitLossSum(account.id),
        )
}
