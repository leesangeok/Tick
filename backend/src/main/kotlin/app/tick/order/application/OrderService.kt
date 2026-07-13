package app.tick.order.application

import app.tick.account.application.LoadAccountPort
import app.tick.account.application.LoadHoldingPort
import app.tick.account.application.SaveAccountPort
import app.tick.account.application.SaveHoldingPort
import app.tick.account.domain.Holding
import app.tick.common.domain.AveragePriceCalculator
import app.tick.common.exception.AccountNotFoundException
import app.tick.common.exception.HoldingNotFoundException
import app.tick.common.exception.StockNotFoundException
import app.tick.order.domain.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OrderService(
    private val loadAccountPort: LoadAccountPort,
    private val saveAccountPort: SaveAccountPort,
    private val loadHoldingPort: LoadHoldingPort,
    private val saveHoldingPort: SaveHoldingPort,
    private val saveOrderPort: SaveOrderPort,
    private val loadStockSummaryPort: LoadStockSummaryPort,
    private val orderEventPublisher: OrderEventPublisherPort,
) : CreateOrderUseCase {

    @Transactional
    override fun buy(command: CreateBuyOrderCommand): CreateOrderResult {
        val stockName = loadStockSummaryPort.nameOf(command.stockCode)
            ?: throw StockNotFoundException(command.stockCode.value)
        val currentPrice = loadStockSummaryPort.currentPrice(command.stockCode)
            ?: throw StockNotFoundException(command.stockCode.value)
        val account = loadAccountPort.loadByMemberId(command.memberId)
            ?: throw AccountNotFoundException(command.memberId)

        val now = Instant.now()
        val order = Order.filledBuy(
            accountId = account.id,
            stockCode = command.stockCode,
            quantity = command.quantity,
            price = currentPrice,
            orderType = command.orderType,
            at = now,
        )

        account.withdraw(order.totalAmount, now)

        val existing = loadHoldingPort.loadByAccountIdAndStockCode(account.id, command.stockCode)
        val holding = existing?.also { it.applyBuy(command.quantity, currentPrice, now) }
            ?: Holding.newFromBuy(account.id, command.stockCode, command.quantity, currentPrice, now)

        saveAccountPort.save(account)
        saveHoldingPort.save(holding)
        val saved = saveOrderPort.save(order)

        val result = toResult(saved, stockName)
        publish(command.memberId, result, saved.createdAt)
        return result
    }

    @Transactional
    override fun sell(command: CreateSellOrderCommand): CreateOrderResult {
        val stockName = loadStockSummaryPort.nameOf(command.stockCode)
            ?: throw StockNotFoundException(command.stockCode.value)
        val currentPrice = loadStockSummaryPort.currentPrice(command.stockCode)
            ?: throw StockNotFoundException(command.stockCode.value)
        val account = loadAccountPort.loadByMemberId(command.memberId)
            ?: throw AccountNotFoundException(command.memberId)
        val holding = loadHoldingPort.loadByAccountIdAndStockCode(account.id, command.stockCode)
            ?: throw HoldingNotFoundException(command.stockCode.value)

        val averageBeforeSell = holding.averagePrice
        val realized = AveragePriceCalculator.realizedProfitLoss(
            sellPrice = currentPrice,
            averagePrice = averageBeforeSell,
            sellQuantity = command.quantity,
        )
        val now = Instant.now()
        val proceeds = currentPrice.multiply(command.quantity)

        holding.applySell(command.quantity, now)
        account.credit(proceeds, now)

        val order = Order.filledSell(
            accountId = account.id,
            stockCode = command.stockCode,
            quantity = command.quantity,
            price = currentPrice,
            orderType = command.orderType,
            averageCostAt = averageBeforeSell,
            realizedProfitLoss = realized,
            at = now,
        )

        saveAccountPort.save(account)
        saveHoldingPort.save(holding)
        val saved = saveOrderPort.save(order)

        val result = toResult(saved, stockName)
        publish(command.memberId, result, saved.createdAt)
        return result
    }

    private fun publish(memberId: Long, result: CreateOrderResult, at: Instant) {
        // 트랜잭션 커밋 전이지만 in-memory publisher 이므로 커밋 실패 시 프론트가 잠깐 잘못된
        // 상태를 보게 될 수 있다. Redis Pub/Sub 도입 시 TransactionSynchronization 로 커밋 후 발행.
        orderEventPublisher.publish(
            OrderExecutedEvent(
                memberId = memberId,
                orderId = result.orderId,
                symbol = result.symbol,
                stockName = result.stockName,
                side = result.side,
                orderType = result.orderType,
                quantity = result.quantity,
                price = result.price,
                totalAmount = result.totalAmount,
                realizedProfitLoss = result.realizedProfitLoss,
                status = result.status,
                at = at.toString(),
            ),
        )
    }

    private fun toResult(order: Order, stockName: String): CreateOrderResult =
        CreateOrderResult(
            orderId = "ord_${order.id}",
            symbol = order.stockCode.value,
            stockName = stockName,
            side = order.side.name,
            orderType = order.orderType.name,
            quantity = order.quantity.toInt,
            price = order.price.value.toInt(),
            totalAmount = order.totalAmount.value,
            realizedProfitLoss = order.realizedProfitLoss?.value,
            status = order.status.name,
            filledAt = order.filledAt?.toString(),
        )
}
