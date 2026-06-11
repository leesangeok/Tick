package app.tick.common.exception

open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class AccountNotFoundException(memberId: Long) :
    BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "계좌를 찾을 수 없습니다. memberId=$memberId")

class InsufficientBalanceException(required: Long, available: Long) :
    BusinessException(
        ErrorCode.INSUFFICIENT_BALANCE,
        "보유 현금이 부족합니다. 필요=$required, 보유=$available",
    )

class StockNotFoundException(stockCode: String) :
    BusinessException(ErrorCode.STOCK_NOT_FOUND, "종목을 찾을 수 없습니다: $stockCode")

class HoldingNotFoundException(stockCode: String) :
    BusinessException(ErrorCode.HOLDING_NOT_FOUND, "보유 종목을 찾을 수 없습니다: $stockCode")

class InsufficientStockQuantityException(required: Int, available: Int) :
    BusinessException(
        ErrorCode.INSUFFICIENT_STOCK_QUANTITY,
        "보유 수량이 부족합니다. 매도=$required, 보유=$available",
    )

class InvalidOrderQuantityException(quantity: Int) :
    BusinessException(ErrorCode.INVALID_ORDER_QUANTITY, "주문 수량은 1 이상이어야 합니다: $quantity")

class InvalidOrderTypeException(orderType: String) :
    BusinessException(ErrorCode.INVALID_ORDER_TYPE, "주문 유형이 올바르지 않습니다: $orderType")
