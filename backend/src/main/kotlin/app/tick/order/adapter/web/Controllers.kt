package app.tick.order.adapter.web

import app.tick.auth.AuthPrincipal
import app.tick.common.domain.Quantity
import app.tick.common.domain.StockCode
import app.tick.common.exception.InvalidOrderQuantityException
import app.tick.common.exception.InvalidOrderTypeException
import app.tick.common.response.ApiResponse
import app.tick.order.application.CreateBuyOrderCommand
import app.tick.order.application.CreateOrderUseCase
import app.tick.order.application.CreateSellOrderCommand
import app.tick.order.application.GetOrdersUseCase
import app.tick.order.domain.OrderType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val createOrder: CreateOrderUseCase,
    private val getOrders: GetOrdersUseCase,
) {
    @PostMapping("/buy")
    fun buy(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @RequestBody request: CreateOrderRequest,
    ): ApiResponse<CreateOrderResponse> =
        ApiResponse.success(createOrder.buy(request.toBuyCommand(principal.memberId)).toResponse())

    @PostMapping("/sell")
    fun sell(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @RequestBody request: CreateOrderRequest,
    ): ApiResponse<CreateOrderResponse> =
        ApiResponse.success(createOrder.sell(request.toSellCommand(principal.memberId)).toResponse())

    @GetMapping
    fun list(@AuthenticationPrincipal principal: AuthPrincipal): ApiResponse<List<OrderResponse>> =
        ApiResponse.success(getOrders.list(principal.memberId).map { it.toResponse() })
}

private fun CreateOrderRequest.toBuyCommand(memberId: Long): CreateBuyOrderCommand =
    CreateBuyOrderCommand(
        memberId = memberId,
        stockCode = StockCode.of(stockCode),
        quantity = validateQuantity(quantity),
        orderType = validateOrderType(orderType),
    )

private fun CreateOrderRequest.toSellCommand(memberId: Long): CreateSellOrderCommand =
    CreateSellOrderCommand(
        memberId = memberId,
        stockCode = StockCode.of(stockCode),
        quantity = validateQuantity(quantity),
        orderType = validateOrderType(orderType),
    )

private fun validateQuantity(quantity: Int): Quantity {
    if (quantity <= 0) throw InvalidOrderQuantityException(quantity)
    return Quantity.ofInt(quantity)
}

private fun validateOrderType(value: String): OrderType =
    runCatching { OrderType.valueOf(value.uppercase()) }
        .getOrElse { throw InvalidOrderTypeException(value) }
