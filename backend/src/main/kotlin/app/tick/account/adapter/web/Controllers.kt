package app.tick.account.adapter.web

import app.tick.account.application.DepositCommand
import app.tick.account.application.DepositUseCase
import app.tick.account.application.GetAccountUseCase
import app.tick.account.application.GetPortfolioUseCase
import app.tick.account.application.GetTransactionsUseCase
import app.tick.auth.AuthPrincipal
import app.tick.common.domain.Money
import app.tick.common.response.ApiResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/account")
class AccountController(
    private val getAccount: GetAccountUseCase,
    private val deposit: DepositUseCase,
) {
    @GetMapping
    fun get(@AuthenticationPrincipal principal: AuthPrincipal): ApiResponse<AccountResponse> =
        ApiResponse.success(getAccount.get(principal.memberId).toResponse())

    @PostMapping("/deposit")
    fun deposit(
        @AuthenticationPrincipal principal: AuthPrincipal,
        @RequestBody request: DepositRequest,
    ): ApiResponse<AccountResponse> =
        ApiResponse.success(
            deposit.deposit(DepositCommand(principal.memberId, Money.of(request.amount))).toResponse(),
        )
}

@RestController
@RequestMapping("/api/v1/portfolio")
class PortfolioController(
    private val getPortfolio: GetPortfolioUseCase,
) {
    @GetMapping
    fun get(@AuthenticationPrincipal principal: AuthPrincipal): ApiResponse<PortfolioResponse> =
        ApiResponse.success(getPortfolio.get(principal.memberId).toResponse())
}

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController(
    private val getTransactions: GetTransactionsUseCase,
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: AuthPrincipal): ApiResponse<List<TransactionResponse>> =
        ApiResponse.success(getTransactions.list(principal.memberId).map { it.toResponse() })
}
