package app.tick.order.application

import app.tick.account.application.LoadAccountPort
import app.tick.account.application.LoadHoldingPort
import app.tick.account.application.SaveAccountPort
import app.tick.account.application.SaveHoldingPort
import app.tick.account.domain.Account
import app.tick.account.domain.Holding
import app.tick.common.domain.Money
import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode
import app.tick.common.exception.AccountNotFoundException
import app.tick.common.exception.HoldingNotFoundException
import app.tick.common.exception.InsufficientBalanceException
import app.tick.common.exception.InsufficientStockQuantityException
import app.tick.common.exception.StockNotFoundException
import app.tick.order.domain.Order
import app.tick.order.domain.OrderSide
import app.tick.order.domain.OrderStatus
import app.tick.order.domain.OrderType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OrderServiceTest {
    private val loadAccount = mockk<LoadAccountPort>()
    private val saveAccount = mockk<SaveAccountPort>(relaxed = true)
    private val loadHolding = mockk<LoadHoldingPort>()
    private val saveHolding = mockk<SaveHoldingPort>(relaxed = true)
    private val saveOrder = mockk<SaveOrderPort>()
    private val loadStock = mockk<LoadStockSummaryPort>()
    private val orderEventPublisher = mockk<OrderEventPublisherPort>(relaxed = true)

    private lateinit var service: OrderService

    private val now = Instant.parse("2026-06-12T00:00:00Z")
    private val stockCode = StockCode.of("005930")
    private val memberId = 1L

    @BeforeEach
    fun setUp() {
        service = OrderService(
            loadAccount, saveAccount, loadHolding, saveHolding, saveOrder, loadStock,
            orderEventPublisher,
        )

        // 기본 스텁: 종목 / 현재가 / 저장 시 그대로 반환
        every { loadStock.nameOf(stockCode) } returns "삼성전자"
        every { loadStock.currentPrice(stockCode) } returns Money.of(70_000L)
        every { saveOrder.save(any()) } answers { firstArg<Order>().withId(99L) }
    }

    private fun newAccount(cash: Long = 10_000_000L): Account =
        Account(
            id = 1L,
            externalId = "test",
            memberId = memberId,
            cash = Money.of(cash),
            totalDeposits = Money.of(cash),
            createdAt = now,
            updatedAt = now,
        )

    private fun newHolding(quantity: Int, averagePrice: Long): Holding =
        Holding(
            id = 1L,
            accountId = 1L,
            stockCode = stockCode,
            quantity = Quantity.ofInt(quantity),
            averagePrice = Money.of(averagePrice),
            createdAt = now,
            updatedAt = now,
        )

    private fun buyCommand(quantity: Int = 10): CreateBuyOrderCommand =
        CreateBuyOrderCommand(memberId, stockCode, Quantity.ofInt(quantity), OrderType.MARKET)

    private fun sellCommand(quantity: Int = 10): CreateSellOrderCommand =
        CreateSellOrderCommand(memberId, stockCode, Quantity.ofInt(quantity), OrderType.MARKET)

    // --- BUY ---

    @Test
    fun `매수 happy - 신규 보유 생성 + 계좌 출금 + 주문 FILLED`() {
        val account = newAccount(cash = 10_000_000L)
        every { loadAccount.loadByMemberId(memberId) } returns account
        every { loadHolding.loadByAccountIdAndStockCode(1L, stockCode) } returns null

        val accountSlot = slot<Account>()
        val holdingSlot = slot<Holding>()
        every { saveAccount.save(capture(accountSlot)) } answers { firstArg() }
        every { saveHolding.save(capture(holdingSlot)) } answers { firstArg() }

        val result = service.buy(buyCommand(quantity = 10))

        // 계좌: 10주 × 70_000 = 700_000 출금
        assertEquals(Money.of(9_300_000L), accountSlot.captured.cash)
        // 신규 보유: 10주 / 평균가 70_000
        assertEquals(Quantity.ofInt(10), holdingSlot.captured.quantity)
        assertEquals(Money.of(70_000L), holdingSlot.captured.averagePrice)

        // 결과
        assertEquals("005930", result.symbol)
        assertEquals("BUY", result.side)
        assertEquals("FILLED", result.status)
        assertEquals(700_000L, result.totalAmount)
        assertNull(result.realizedProfitLoss)
    }

    @Test
    fun `매수 - 기존 보유에 누적 (가중평균 갱신)`() {
        val account = newAccount(cash = 10_000_000L)
        val holding = newHolding(quantity = 10, averagePrice = 50_000L)
        every { loadAccount.loadByMemberId(memberId) } returns account
        every { loadHolding.loadByAccountIdAndStockCode(1L, stockCode) } returns holding

        val holdingSlot = slot<Holding>()
        every { saveAccount.save(any()) } answers { firstArg() }
        every { saveHolding.save(capture(holdingSlot)) } answers { firstArg() }

        service.buy(buyCommand(quantity = 10))

        // 가중평균: (50_000 * 10 + 70_000 * 10) / 20 = 60_000
        assertEquals(Quantity.ofInt(20), holdingSlot.captured.quantity)
        assertEquals(Money.of(60_000L), holdingSlot.captured.averagePrice)
    }

    @Test
    fun `매수 잔고 부족이면 InsufficientBalanceException`() {
        val account = newAccount(cash = 100_000L) // 10 × 70_000 = 700_000 필요
        every { loadAccount.loadByMemberId(memberId) } returns account
        every { loadHolding.loadByAccountIdAndStockCode(1L, stockCode) } returns null

        assertThrows<InsufficientBalanceException> { service.buy(buyCommand(quantity = 10)) }
        verify(exactly = 0) { saveAccount.save(any()) }
        verify(exactly = 0) { saveHolding.save(any()) }
        verify(exactly = 0) { saveOrder.save(any()) }
    }

    @Test
    fun `매수 - 종목이 없으면 StockNotFoundException`() {
        every { loadStock.nameOf(stockCode) } returns null
        assertThrows<StockNotFoundException> { service.buy(buyCommand()) }
    }

    @Test
    fun `매수 - 계좌가 없으면 AccountNotFoundException`() {
        every { loadAccount.loadByMemberId(memberId) } returns null
        assertThrows<AccountNotFoundException> { service.buy(buyCommand()) }
    }

    // --- SELL ---

    @Test
    fun `매도 happy - 보유 차감 + 계좌 입금 + 실현손익 기록`() {
        val account = newAccount(cash = 1_000_000L)
        val holding = newHolding(quantity = 20, averagePrice = 50_000L)
        every { loadAccount.loadByMemberId(memberId) } returns account
        every { loadHolding.loadByAccountIdAndStockCode(1L, stockCode) } returns holding

        val accountSlot = slot<Account>()
        val holdingSlot = slot<Holding>()
        val orderSlot = slot<Order>()
        every { saveAccount.save(capture(accountSlot)) } answers { firstArg() }
        every { saveHolding.save(capture(holdingSlot)) } answers { firstArg() }
        every { saveOrder.save(capture(orderSlot)) } answers { firstArg<Order>().withId(99L) }

        // 매도 10주 × 현재가 70_000 = 700_000 입금
        // 실현손익 = (70_000 - 50_000) × 10 = 200_000
        val result = service.sell(sellCommand(quantity = 10))

        assertEquals(Money.of(1_700_000L), accountSlot.captured.cash)
        assertEquals(Quantity.ofInt(10), holdingSlot.captured.quantity)
        assertEquals(Money.of(50_000L), holdingSlot.captured.averagePrice) // 평균가는 그대로

        assertEquals(OrderSide.SELL, orderSlot.captured.side)
        assertEquals(OrderStatus.FILLED, orderSlot.captured.status)
        assertEquals(Money.of(50_000L), orderSlot.captured.averageCostAt)
        assertNotNull(orderSlot.captured.realizedProfitLoss)
        assertEquals(200_000L, orderSlot.captured.realizedProfitLoss!!.value)

        assertEquals(200_000L, result.realizedProfitLoss)
        assertEquals(700_000L, result.totalAmount)
    }

    @Test
    fun `매도 - 매도가가 평균가보다 낮으면 실현 손실 (음수)`() {
        val account = newAccount(cash = 0L)
        val holding = newHolding(quantity = 10, averagePrice = 80_000L)
        every { loadAccount.loadByMemberId(memberId) } returns account
        every { loadHolding.loadByAccountIdAndStockCode(1L, stockCode) } returns holding

        val orderSlot = slot<Order>()
        every { saveAccount.save(any()) } answers { firstArg() }
        every { saveHolding.save(any()) } answers { firstArg() }
        every { saveOrder.save(capture(orderSlot)) } answers { firstArg<Order>().withId(99L) }

        // (70_000 - 80_000) × 5 = -50_000
        val result = service.sell(sellCommand(quantity = 5))

        assertEquals(-50_000L, result.realizedProfitLoss)
        assertEquals(-50_000L, orderSlot.captured.realizedProfitLoss!!.value)
    }

    @Test
    fun `매도 - 보유 없으면 HoldingNotFoundException`() {
        every { loadAccount.loadByMemberId(memberId) } returns newAccount()
        every { loadHolding.loadByAccountIdAndStockCode(1L, stockCode) } returns null

        assertThrows<HoldingNotFoundException> { service.sell(sellCommand()) }
    }

    @Test
    fun `매도 - 보유 수량 부족이면 InsufficientStockQuantityException`() {
        every { loadAccount.loadByMemberId(memberId) } returns newAccount()
        every { loadHolding.loadByAccountIdAndStockCode(1L, stockCode) } returns newHolding(quantity = 3, averagePrice = 50_000L)

        assertThrows<InsufficientStockQuantityException> { service.sell(sellCommand(quantity = 10)) }
        verify(exactly = 0) { saveAccount.save(any()) }
        verify(exactly = 0) { saveOrder.save(any()) }
    }
}

// Order 도메인에 id 만 바꾼 복사본 만들기 (factory 테스트 helper)
private fun Order.withId(newId: Long): Order = Order(
    id = newId,
    accountId = accountId,
    stockCode = stockCode,
    side = side,
    orderType = orderType,
    quantity = quantity,
    price = price,
    filledQuantity = filledQuantity,
    status = status,
    averageCostAt = averageCostAt,
    realizedProfitLoss = realizedProfitLoss,
    createdAt = createdAt,
    filledAt = filledAt,
)
