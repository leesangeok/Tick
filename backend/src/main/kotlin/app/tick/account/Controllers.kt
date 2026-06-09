package app.tick.account

import app.tick.auth.AuthPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account")
class AccountController(
    private val accountService: AccountService,
) {
    @GetMapping
    fun get(@AuthenticationPrincipal principal: AuthPrincipal): AccountResponse =
        accountService.get(principal.memberId)

    @PostMapping("/deposit")
    fun deposit(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @RequestBody request: DepositRequest,
    ): AccountResponse = accountService.deposit(principal.memberId, request.amount)
}

@RestController
@RequestMapping("/api/portfolio")
class PortfolioController(
    private val portfolioService: PortfolioService,
) {
    @GetMapping
    fun get(@AuthenticationPrincipal principal: AuthPrincipal): PortfolioResponse =
        portfolioService.getPortfolio(principal.memberId)
}

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderHistoryService: OrderHistoryService,
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: AuthPrincipal): List<OrderResponse> =
        orderHistoryService.list(principal.memberId)
}

@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val transactionService: TransactionService,
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: AuthPrincipal): List<TransactionResponse> =
        transactionService.list(principal.memberId)
}
