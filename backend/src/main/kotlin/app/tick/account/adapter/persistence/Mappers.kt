package app.tick.account.adapter.persistence

import app.tick.account.domain.Account
import app.tick.account.domain.Deposit
import app.tick.account.domain.DepositType
import app.tick.account.domain.Holding
import app.tick.common.domain.Money
import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode

object AccountMapper {
    fun toDomain(entity: AccountJpaEntity): Account = Account(
        id = entity.id,
        externalId = entity.externalId,
        memberId = entity.memberId,
        cash = Money.of(entity.cash),
        totalDeposits = Money.of(entity.totalDeposits),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun toEntity(domain: Account): AccountJpaEntity = AccountJpaEntity(
        id = domain.id,
        externalId = domain.externalId,
        memberId = domain.memberId,
        cash = domain.cash.value,
        totalDeposits = domain.totalDeposits.value,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt,
    )
}

object HoldingMapper {
    fun toDomain(entity: HoldingJpaEntity): Holding = Holding(
        id = entity.id,
        accountId = entity.accountId,
        stockCode = StockCode.of(entity.symbol),
        quantity = Quantity.ofInt(entity.quantity),
        averagePrice = Money.ofInt(entity.averagePrice),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    fun toEntity(domain: Holding): HoldingJpaEntity = HoldingJpaEntity(
        id = domain.id,
        accountId = domain.accountId,
        symbol = domain.stockCode.value,
        quantity = domain.quantity.toInt,
        averagePrice = domain.averagePrice.value.toInt(),
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt,
    )
}

object DepositMapper {
    fun toDomain(entity: DepositHistoryJpaEntity): Deposit = Deposit(
        id = entity.id,
        accountId = entity.accountId,
        amount = Money.of(entity.amount),
        type = if (entity.type == "WITHDRAW") DepositType.WITHDRAW else DepositType.DEPOSIT,
        createdAt = entity.createdAt,
    )

    fun toEntity(domain: Deposit): DepositHistoryJpaEntity = DepositHistoryJpaEntity(
        id = domain.id,
        accountId = domain.accountId,
        amount = domain.amount.value,
        type = domain.type.name,
        createdAt = domain.createdAt,
    )
}
