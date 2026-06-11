package app.tick.order.adapter.persistence

import app.tick.common.domain.Money
import app.tick.common.domain.ProfitLoss
import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode
import app.tick.order.domain.Order
import app.tick.order.domain.OrderSide
import app.tick.order.domain.OrderStatus
import app.tick.order.domain.OrderType

object OrderMapper {
    fun toDomain(entity: OrderJpaEntity): Order = Order(
        id = entity.id,
        accountId = entity.accountId,
        stockCode = StockCode.of(entity.symbol),
        side = OrderSide.valueOf(entity.side),
        orderType = OrderType.valueOf(entity.orderType),
        quantity = Quantity.ofInt(entity.quantity),
        price = Money.ofInt(entity.price),
        filledQuantity = entity.filledQuantity?.let { Quantity.ofInt(it) },
        status = OrderStatus.valueOf(entity.status),
        averageCostAt = entity.averageCostAt?.let { Money.ofInt(it) },
        realizedProfitLoss = entity.realizedProfitLoss?.let { ProfitLoss.of(it) },
        createdAt = entity.createdAt,
        filledAt = entity.filledAt,
    )

    fun toEntity(domain: Order): OrderJpaEntity = OrderJpaEntity(
        id = domain.id,
        accountId = domain.accountId,
        symbol = domain.stockCode.value,
        side = domain.side.name,
        orderType = domain.orderType.name,
        quantity = domain.quantity.toInt,
        price = domain.price.value.toInt(),
        filledQuantity = domain.filledQuantity?.toInt,
        status = domain.status.name,
        averageCostAt = domain.averageCostAt?.value?.toInt(),
        realizedProfitLoss = domain.realizedProfitLoss?.value,
        createdAt = domain.createdAt,
        filledAt = domain.filledAt,
    )
}
