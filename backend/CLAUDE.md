# CLAUDE.md

## Project Overview

`tick`은 실시간 주식 모의투자 플랫폼이다.

사용자는 실제 돈을 사용하지 않고 가상 자산으로 주식을 매수/매도하며, 실시간 시세를 기반으로 포트폴리오 평가금액, 손익, 수익률을 확인할 수 있다. 또한 뉴스, 주가 변동 데이터, 시장 정보를 기반으로 AI가 주가 상승/하락 이유를 요약해주는 기능을 제공한다.

이 저장소는 `tick` 서비스의 **Backend API 서버**이다. 프론트엔드(Next.js)는 `/Users/leesangeok/Documents/workspace/Tick/frontend` 에 있으며 본 백엔드의 REST API 를 호출한다.

---

## Project State Snapshot (현재 상태)

이 문서는 미래의 이상적 구조와 함께 현재 실제 코드 상태를 함께 다룬다. AI 에이전트는 "현재 적용 / 다음 단계 / 미래" 라벨을 보고 어디까지가 즉시 따라야 할 규칙인지 판단해야 한다.

### 패키지 / 빌드 (현재)

- 베이스 패키지: **`app.tick`** (`com.tick.backend` 아님)
- 빌드: Spring Boot **3.5.2**, Kotlin **1.9.25**, Gradle Kotlin DSL, Java 21
- 현재 의존성: `web`, `data-jpa`, `validation`, `flyway-core`, `flyway-database-postgresql`, `postgresql`, `actuator`, `jackson-module-kotlin`, `kotlin-reflect`
- 아직 **없는** 것: Spring Security, Spring Kafka, Spring Data Redis, QueryDSL, Testcontainers, Springdoc OpenAPI, OAuth2 Client

### 디렉터리 구조 (현재)

```text
backend/
├── compose.yaml                              Postgres 16 (OrbStack)
├── build.gradle.kts
├── src/main/resources/
│   ├── application.yml                       datasource, jpa, flyway, server, tick.cors
│   └── db/migration/
│       ├── V1__stock_master.sql              10개 종목 시드
│       └── V2__account_holding_order.sql     account/holding/order_history/deposit_history + demo 계정 시드
└── src/main/kotlin/app/tick/
    ├── TickApplication.kt
    ├── config/CorsConfig.kt
    ├── stock/                                StockMaster, StockMasterRepository, StockPriceGenerator,
    │                                         StockDto, StockService, StockController
    └── account/                              Account, Holding, OrderHistory, DepositHistory, Repositories,
                                              Dto, AccountService, PortfolioService, OrderHistoryService,
                                              TransactionService, Controllers
```

### 핵심 파일 인덱스 (절대경로)

- 도메인 엔티티: `backend/src/main/kotlin/app/tick/account/{Account,Holding,OrderHistory,DepositHistory}.kt`
- 종목 마스터: `backend/src/main/kotlin/app/tick/stock/StockMaster.kt`
- 가격 생성기 (프론트 mulberry32 와 동일한 알고리즘): `backend/src/main/kotlin/app/tick/stock/StockPriceGenerator.kt`
- CORS: `backend/src/main/kotlin/app/tick/config/CorsConfig.kt`
- 마이그레이션: `backend/src/main/resources/db/migration/`
- 앱 설정: `backend/src/main/resources/application.yml`

### 현재 API 엔드포인트

```text
GET    /api/stocks
GET    /api/stocks/{symbol}
GET    /api/stocks/{symbol}/prices?days=N
GET    /api/account
POST   /api/account/deposit
GET    /api/portfolio
GET    /api/orders
GET    /api/transactions
```

`/api/v1/*` 가 아니라 `/api/*` 이다. v1 prefix 도입은 카카오 인증 단계에서 일괄 진행.

### 사용자 식별 (현재)

`Member` 도메인이 아직 없다. `account.external_id = 'demo'` 하드코딩으로 단일 계정만 사용. 카카오 OAuth 도입 시점에 `Member` 추가 + `Account.memberId` FK 추가 + `external_id` 폐기.

### Spring/Kotlin 버전 참고

Spring Initializr 가 AGENTS.md 를 생성하지 않으므로 (Next.js 만), 버전별 변경사항은 다음을 직접 참조:

- Spring Boot 3.5 reference: https://docs.spring.io/spring-boot/docs/3.5.2/reference/html/
- Kotlin 1.9 변경점: 표준 라이브러리/컴파일러 옵션
- JPA 3.x (Hibernate 6.6): EntityGraph, fetch join 권장

업그레이드 시 본 CLAUDE.md 상단에 마이그레이션 노트 섹션 추가.

---

## Phase Roadmap

| 영역 | 현재 (Phase 1-2) | 다음 (Phase 3) | 미래 (Phase 4+) |
|---|---|---|---|
| 구조 | layered + package-per-domain | account/order 도메인부터 점진적 hexagonal 전환 | 전 도메인 hexagonal |
| 인증 | `demo` 하드코딩 | 카카오 OAuth + JWT + Member | RBAC, refresh rotation, 다중 OAuth provider |
| 저장소 | Postgres + Flyway | + Redis (refresh token, 시세 캐시) | + Kafka (이벤트), pgvector (RAG) |
| 외부 연동 | 없음 (mulberry32 가짜 시세) | LLM 클라이언트 (AI 리포트) | 실시간 시세 API (한투/키움), News API |
| 통신 | REST | + WebSocket/SSE (실시간 시세) | + Kafka stream |
| API prefix | `/api/*` | `/api/v1/*` 로 일괄 전환 | — |
| 테스트 | JUnit5 (없음) | + MockK + Testcontainers | + Contract test |
| 관찰성 | Actuator health/info | + 구조화 로그 | + Micrometer + OpenTelemetry |

본 문서에서 **Phase 4** 로 표시된 섹션은 가이드만 제공하며 현재 코드엔 미적용. 도입 시점에 본 섹션의 규칙을 적용한다.

---

## Main Architecture

이 프로젝트의 **목표 아키텍처**는 **DDD 기반 도메인 중심 설계 + Hexagonal Architecture 의 Port/Adapter 패턴** 이다.

현재 (Phase 1-2) 는 layered + 도메인 패키지 구조. account/order 같은 핵심 도메인부터 점진적으로 hexagonal 로 전환한다. 단순 CRUD 도메인(stock 등)은 layered 유지 가능.

핵심 목표:

- 핵심 비즈니스 로직을 외부 기술로부터 분리한다
- 주문, 계좌, 보유 종목, 포트폴리오 계산 로직을 도메인 중심으로 설계한다
- 외부 주식 API, Kafka, Redis, AI Client, WebSocket 등 변경 가능성이 높은 기술 의존성은 Port 로 추상화한다
- Application Service 만 읽어도 유스케이스 흐름을 이해할 수 있도록 작성한다
- Controller, Kafka Consumer, Scheduler 등 외부 진입점은 Inbound Adapter 로 둔다
- DB, Kafka, Redis, 외부 API, AI Client 는 Outbound Adapter 로 둔다

---

## Architecture Principles

반드시 지켜야 할 원칙:

- Domain 은 Spring, JPA, Kafka, Redis, WebClient 에 의존하지 않는다
- Application 은 UseCase 흐름과 Transaction 을 조율한다
- Application 은 Outbound Port interface 에 의존한다
- Adapter 는 Port interface 를 구현한다
- Presentation 계층은 HTTP 요청/응답만 담당한다
- Controller 에는 비즈니스 로직을 작성하지 않는다
- Domain 에는 핵심 비즈니스 규칙을 둔다
- **단순 CRUD 에도 무조건 Port/Adapter 를 남발하지 않는다**
- 핵심 도메인인 Account, Order, Holding, Trade, Portfolio, StockPrice, AiSummary 에는 DDD + Hexagonal 구조를 우선 적용한다
- Watchlist 처럼 단순한 기능은 구조를 과도하게 쪼개지 않는다

---

## Tech Stack

### 현재 적용

- Language: **Kotlin 1.9.25**
- Framework: **Spring Boot 3.5.2** (web, data-jpa, validation, actuator)
- Build: **Gradle Kotlin DSL**
- DB: **PostgreSQL 16** (docker compose)
- Migration: **Flyway 10** (`flyway-core` + `flyway-database-postgresql`)
- ORM: Spring Data JPA + Hibernate 6.6
- JSON: jackson-module-kotlin
- Runtime: Java 21

### Phase 3 도입 예정

- Security: Spring Security + Spring Boot Starter OAuth2 Client (카카오)
- JWT: `io.jsonwebtoken:jjwt` 또는 `nimbus-jose-jwt`
- Cache: Redis (Spring Data Redis)
- 테스트: JUnit5 + MockK + Testcontainers
- API Docs: Springdoc OpenAPI (선택)

### Phase 4 도입 예정

- Event Streaming: Kafka (Spring Kafka)
- LLM: OpenAI / Anthropic SDK (port 뒤로 캡슐화)
- 시세 API: 한투/키움 등 (port 뒤)
- pgvector: AI 리포트 RAG
- Observability: Micrometer + OpenTelemetry
- Infra Target: Docker, AWS EC2/ECS, RDS, ElastiCache, MSK

---

## Domain Overview

| 도메인 | 책임 | 현재 상태 |
|---|---|---|
| Auth | 인증, JWT, Refresh Token | Phase 3 |
| Member | 회원 | Phase 3 (카카오 OAuth 와 함께) |
| Account | 모의투자 계좌, 가상 현금 | ✅ 구현됨 (단일 demo) |
| Stock | 종목 정보, 시세 | ✅ 구현됨 (가짜 가격) |
| Order | 매수/매도 주문 | ✅ 조회만 구현됨 (POST 는 Phase 2-끝) |
| Holding | 보유 종목 | ✅ 구현됨 |
| Trade | 체결 내역 | (현재 OrderHistory 에 흡수) |
| Portfolio | 평가금액, 손익, 수익률 | ✅ 구현됨 (조회 모델) |
| Watchlist | 관심 종목 | Phase 3 |
| Market | 실시간 시세 이벤트, WebSocket Push | Phase 4 |
| News | 종목 관련 뉴스 | Phase 4 (현재 frontend mock) |
| AiSummary | 주가 상승/하락 이유 요약 | Phase 4 (현재 frontend mock) |

---

## Package Structure

### 현재 (Phase 1-2)

```text
app.tick
 ├── config
 │   └── CorsConfig
 ├── stock
 │   ├── StockMaster (Entity)
 │   ├── StockMasterRepository
 │   ├── StockPriceGenerator
 │   ├── StockDto
 │   ├── StockService
 │   └── StockController
 └── account
     ├── Account / Holding / OrderHistory / DepositHistory (Entity)
     ├── Repositories
     ├── Dto
     ├── AccountService / PortfolioService / OrderHistoryService / TransactionService
     └── Controllers
```

### 목표 (Phase 3+, 도메인별 hexagonal)

```text
app.tick
 ├── common
 │   ├── config
 │   ├── exception
 │   ├── response
 │   ├── security
 │   └── util
 ├── auth
 ├── member
 ├── account
 ├── stock
 ├── order
 ├── portfolio
 ├── market
 ├── news
 └── ai
```

핵심 도메인의 hexagonal 구조 (예: `order`):

```text
order
 ├── adapter
 │   ├── in
 │   │   ├── web/OrderController
 │   │   └── kafka/OrderEventConsumer
 │   └── out
 │       ├── persistence/{OrderPersistenceAdapter, OrderJpaRepository, OrderJpaEntity, OrderMapper}
 │       ├── kafka/OrderEventKafkaAdapter
 │       └── external/StockPriceApiAdapter
 ├── application
 │   ├── port
 │   │   ├── in/{CreateOrderUseCase, CancelOrderUseCase, GetOrderUseCase}
 │   │   └── out/{LoadAccountPort, SaveAccountPort, LoadHoldingPort, SaveHoldingPort,
 │   │            SaveOrderPort, SaveTradePort, LoadStockPricePort, PublishOrderEventPort}
 │   ├── service/OrderService
 │   ├── command/{CreateBuyOrderCommand, CreateSellOrderCommand}
 │   └── result/CreateOrderResult
 └── domain
     ├── Order
     ├── OrderType
     ├── OrderStatus
     ├── OrderCompletedEvent
     ├── OrderExecutionPolicy
     └── exception
```

> 패키지명 `adapter.in.web` 의 `in` 은 Kotlin 키워드와 충돌하지 않는다 (패키지 식별자에선 reserved 가 아님). 가독성을 위해 `inbound/outbound` 사용도 무방. 본 문서는 `in/out` 으로 통일.

모든 도메인에 이 구조를 기계적으로 강제하지 않는다. 비즈니스 규칙이 적은 단순 조회/CRUD 도메인은 layered 유지 가능.

---

## Dependency Rule

의존성 방향은 안쪽을 향한다.

```text
adapter.in.web        → application.port.in
application.port.in   → application.service
application.service   → domain
application.service   → application.port.out (interface)
adapter.out.*         → application.port.out (구현)
```

금지되는 의존성:

```text
domain → application
domain → adapter
domain → infrastructure
domain → Spring
domain → JPA Repository
domain → Kafka
domain → Redis
domain → WebClient
```

허용되는 의존성:

```text
application → domain
application → port.out interface
adapter.in  → port.in interface
adapter.out → port.out interface
adapter.out → external technology
```

---

## DDD Rules

### Entity

Entity 는 식별자를 가지며 생명주기를 가진다. 무분별한 setter 를 열지 않고, 상태 변경은 의미 있는 도메인 메서드로 표현한다.

```kotlin
class Account(
    val id: Long,
    val memberId: Long,
    private var cash: Money,
) {
    fun withdraw(amount: Money) {
        if (cash.isLessThan(amount)) throw InsufficientBalanceException()
        cash = cash.minus(amount)
    }

    fun deposit(amount: Money) {
        cash = cash.plus(amount)
    }

    fun getCash(): Money = cash
}
```

Entity 예시: `Member`, `Account`, `Order`, `Holding`, `Trade`.

---

### Value Object

값 자체가 중요한 개념은 Value Object 로 만든다. 불변이다.

#### Money — 절대값 (≥ 0)

현금/평가금액/단가/주문금액 등 **항상 양수 또는 0** 인 금액에 사용.

```kotlin
@JvmInline
value class Money(val value: Long) {
    init { require(value >= 0) { "금액은 0 이상이어야 합니다." } }

    operator fun plus(other: Money) = Money(value + other.value)
    fun minus(other: Money): Money {
        require(value >= other.value) { "차감 금액이 현재 금액보다 큽니다." }
        return Money(value - other.value)
    }
    fun multiply(quantity: Quantity) = Money(value * quantity.value)
    fun isLessThan(other: Money) = value < other.value

    companion object {
        val ZERO = Money(0L)
    }
}
```

#### ProfitLoss — 부호 허용 (음수/양수)

손익(`profitLoss`, `realizedProfitLoss`, `changeAmount`) 같이 음수가 의미 있는 값에 사용. Money 에 음수를 허용하지 않는 대신 별도 VO 로 표현한다.

```kotlin
@JvmInline
value class ProfitLoss(val value: Long) {
    operator fun plus(other: ProfitLoss) = ProfitLoss(value + other.value)
    operator fun minus(other: ProfitLoss) = ProfitLoss(value - other.value)
    operator fun unaryMinus() = ProfitLoss(-value)
    val isPositive: Boolean get() = value > 0
    val isNegative: Boolean get() = value < 0

    companion object {
        val ZERO = ProfitLoss(0L)
        fun between(after: Money, before: Money) = ProfitLoss(after.value - before.value)
    }
}
```

#### Quantity — 0 허용

보유 0주 표현 가능. 매수/매도 주문 수량처럼 ">0" 이어야 하는 경우는 호출 측에서 검증.

```kotlin
@JvmInline
value class Quantity(val value: Long) {
    init { require(value >= 0) { "수량은 0 이상이어야 합니다." } }

    operator fun plus(other: Quantity) = Quantity(value + other.value)
    fun minus(other: Quantity): Quantity {
        require(value >= other.value) { "차감 수량이 현재 수량보다 큽니다." }
        return Quantity(value - other.value)
    }
    val isZero: Boolean get() = value == 0L

    companion object {
        val ZERO = Quantity(0L)
        fun positive(value: Long): Quantity {
            require(value > 0) { "수량은 0보다 커야 합니다." }
            return Quantity(value)
        }
    }
}
```

#### 기타 VO

- `StockCode(val value: String)` — symbol (`005930` 등)
- `Rate(val value: Double)` — 수익률, 등락률 (%, 소수점 그대로)
- `OrderPrice` — 주문가 (지정가/시장가 차이를 표현하려면 sealed class)

---

### Aggregate

Aggregate 는 일관성 경계를 기준으로 설계한다. 초기 후보:

```text
Account Aggregate     — Account, CashHistory
Order Aggregate       — Order, Trade
Holding Aggregate     — Holding
Portfolio             — 조회 모델 (aggregate 아님)
```

주문 체결 시 Account, Holding, Order, Trade 가 함께 변경된다. 여러 Aggregate 변경은 Application Service 에서 하나의 transaction 으로 조율한다.

### Domain Service

특정 Entity 하나에 넣기 애매한 순수 도메인 로직은 Domain Service 로 분리한다. **Spring Bean 으로 만들지 않는다.** 순수 Kotlin class/object 로 유지.

```kotlin
class AveragePriceCalculator {
    fun calculate(
        currentQuantity: Quantity,
        currentAveragePrice: Money,
        buyQuantity: Quantity,
        buyPrice: Money,
    ): Money {
        val currentTotal = currentAveragePrice.value * currentQuantity.value
        val buyTotal = buyPrice.value * buyQuantity.value
        val totalQuantity = currentQuantity.value + buyQuantity.value
        return Money((currentTotal + buyTotal) / totalQuantity)
    }
}
```

예시: `AveragePriceCalculator`, `PortfolioCalculator`, `OrderExecutionPolicy`.

### Domain Event

도메인에서 의미 있는 사건은 Domain Event 로 표현한다.

예시: `OrderCreatedEvent`, `OrderCompletedEvent`, `OrderCanceledEvent`, `StockPriceUpdatedEvent`, `PortfolioUpdatedEvent`, `AiSummaryRequestedEvent`.

**초기 구현 (Phase 2-3)**: Application Service 에서 event 생성 후 Publish Port 호출. **Phase 4**: Outbox Pattern 으로 확장.

---

## Hexagonal Architecture Rules

### Inbound Adapter

외부에서 애플리케이션을 호출하는 진입점. REST Controller, WebSocket Handler, Kafka Consumer, Scheduler 등.

Inbound Adapter 는 Application Port In (UseCase interface) 을 호출한다.

```kotlin
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
) {
    @PostMapping("/buy")
    fun createBuyOrder(
        @RequestBody request: CreateBuyOrderRequest,
        @AuthenticationPrincipal principal: AuthPrincipal,
    ): ApiResponse<CreateOrderResponse> {
        val command = request.toCommand(principal.memberId)
        val response = createOrderUseCase.createBuyOrder(command)
        return ApiResponse.success(response)
    }
}
```

> Phase 3 에서 `/api/orders` → `/api/v1/orders` 로 일괄 전환.

### Inbound Port (UseCase)

```kotlin
interface CreateOrderUseCase {
    fun createBuyOrder(command: CreateBuyOrderCommand): CreateOrderResult
    fun createSellOrder(command: CreateSellOrderCommand): CreateOrderResult
}
```

UseCase 는 기능 단위. 예시: `CreateOrderUseCase`, `CancelOrderUseCase`, `GetPortfolioUseCase`, `GetCurrentStockPriceUseCase`, `SummarizeStockReasonUseCase`.

단순 CRUD 에 과도하게 UseCase 를 쪼개지 않아도 된다.

### Application Service

Application Service 는 Inbound Port 를 구현. 역할:

- transaction 관리
- use case 흐름 제어
- domain 객체 조회 / 생성
- domain 메서드 호출
- outbound port 호출 (저장, 이벤트 발행)
- 권한 검증

**Application Service 에는 복잡한 계산 로직을 직접 작성하지 않는다.** 계산과 상태 변경 규칙은 Domain Entity, Value Object, Domain Service 에 위임한다.

---

## Application Service Readability Rule

Application Service 는 유스케이스의 흐름이 한눈에 읽히도록 작성한다. 좋은 Application Service 는 메서드 하나만 읽어도 다음을 이해할 수 있다:

- 어떤 데이터를 조회하는지
- 어떤 외부 데이터를 가져오는지
- 어떤 도메인 객체를 생성하는지
- 어떤 비즈니스 행위를 수행하는지
- 어떤 상태 변경이 발생하는지
- 어떤 데이터를 저장하는지
- 어떤 이벤트를 발행하는지
- 어떤 결과를 반환하는지

### Good Example

```kotlin
@Service
class OrderService(
    private val loadAccountPort: LoadAccountPort,
    private val saveAccountPort: SaveAccountPort,
    private val loadHoldingPort: LoadHoldingPort,
    private val saveHoldingPort: SaveHoldingPort,
    private val saveOrderPort: SaveOrderPort,
    private val saveTradePort: SaveTradePort,
    private val loadStockPricePort: LoadStockPricePort,
    private val publishOrderEventPort: PublishOrderEventPort,
) : CreateOrderUseCase {

    @Transactional
    override fun createBuyOrder(command: CreateBuyOrderCommand): CreateOrderResult {
        val account      = loadAccountPort.loadByMemberId(command.memberId)
        val currentPrice = loadStockPricePort.loadCurrentPrice(command.stockCode)
        val holding      = loadHoldingPort.loadOrEmpty(command.memberId, command.stockCode)

        val order = Order.buy(
            memberId  = command.memberId,
            stockCode = command.stockCode,
            quantity  = command.quantity,
            price     = currentPrice,
        )

        account.withdraw(order.totalAmount())
        holding.applyBuy(order)
        order.complete()

        val trade = Trade.from(order)

        saveAccountPort.save(account)
        saveHoldingPort.save(holding)
        saveOrderPort.save(order)
        saveTradePort.save(trade)

        publishOrderEventPort.publishOrderCompleted(OrderCompletedEvent.from(order))

        return CreateOrderResult.from(order)
    }
}
```

이 코드는 서비스 메서드만 읽어도 매수 주문의 전체 흐름이 보인다.

```text
계좌 조회 → 현재가 조회 → 보유 종목 조회
→ 매수 주문 생성 → 계좌 출금 → 보유 종목 반영 → 주문 체결
→ 체결 내역 생성 → 저장 → 이벤트 발행 → 결과 반환
```

### Bad Example

```kotlin
@Transactional
override fun createBuyOrder(command: CreateBuyOrderCommand): CreateOrderResult {
    val account = accountRepository.findByMemberId(command.memberId)
        ?: throw AccountNotFoundException()
    val currentPrice = stockApiClient.getCurrentPrice(command.stockCode)
    val totalAmount = currentPrice * command.quantity

    if (account.cash < totalAmount) throw InsufficientBalanceException()
    account.cash -= totalAmount

    val holding = holdingRepository.findByMemberIdAndStockCode(command.memberId, command.stockCode)
    if (holding == null) {
        holdingRepository.save(Holding(/* ... */))
    } else {
        val currentTotal = holding.averagePrice * holding.quantity
        val buyTotal = currentPrice * command.quantity
        val totalQuantity = holding.quantity + command.quantity
        holding.averagePrice = (currentTotal + buyTotal) / totalQuantity
        holding.quantity += command.quantity
    }

    val order = Order(/* ... */)
    orderRepository.save(order)
    kafkaTemplate.send("order.completed", order.id.toString(), order)
    return CreateOrderResult(order.id)
}
```

문제점:
- Service 가 Repository/외부 Client/KafkaTemplate 에 직접 의존
- 평균 매입 단가 계산, 계좌 출금 규칙, Holding 생성/수정 규칙이 Service 에 노출
- 테스트 어려움
- 유스케이스 흐름보다 세부 구현이 먼저 보임

---

## Service Method Structure Rule

Application Service 메서드는 가능한 한 다음 순서로 작성:

```text
1. Command 검증
2. 필요한 Aggregate / Domain 조회
3. 외부 데이터 조회
4. Domain 객체 생성
5. Domain 메서드를 통한 상태 변경
6. 저장
7. 이벤트 발행
8. Result 반환
```

---

## Domain Method Naming Rule

비즈니스 의미가 드러나는 이름.

좋은 예: `account.withdraw(amount)`, `account.deposit(amount)`, `holding.applyBuy(order)`, `holding.applySell(order)`, `order.complete()`, `order.cancel()`, `portfolio.calculateProfitRate(currentPrices)`

나쁜 예: `account.updateCash()`, `holding.update()`, `order.changeStatus()`, `portfolio.calculate()`

---

## Business Logic Placement Rule

| 둘 곳 | 어떤 로직 | 예시 |
|---|---|---|
| Entity | 특정 Entity 상태 변경 / 불변성 유지 | `Account.withdraw()`, `Order.complete()`, `Holding.applyBuy()` |
| Value Object | 값 계산, 검증, 산술 연산 | `Money.plus()`, `ProfitLoss.between()`, `Quantity.minus()` |
| Domain Service | 여러 도메인 객체가 필요한 순수 계산/정책 | `AveragePriceCalculator`, `PortfolioCalculator`, `OrderExecutionPolicy` |
| Application Service | 유스케이스 흐름 제어, 트랜잭션, 포트 호출, 저장, 이벤트 발행 | 위 Good Example |

Application Service 에는 복잡한 if/else 계산 로직을 길게 작성하지 않는다.

---

## Outbound Port Rules

Application 이 외부 시스템/저장소에 의존해야 할 때 interface 로 정의.

```kotlin
interface LoadStockPricePort {
    fun loadCurrentPrice(stockCode: StockCode): Money
}

interface PublishOrderEventPort {
    fun publishOrderCompleted(event: OrderCompletedEvent)
}

interface AiSummaryPort {
    fun summarize(command: AiSummaryCommand): AiSummaryResult
}
```

대상: DB 저장/조회, 외부 주식 API, Kafka publish, Redis cache, AI Client, News API, WebSocket Push, JWT Provider.

## Outbound Adapter Rules

Outbound Port 의 실제 구현체.

```kotlin
@Component
class StockPriceApiAdapter(
    private val webClient: WebClient,
) : LoadStockPricePort {
    override fun loadCurrentPrice(stockCode: StockCode): Money {
        TODO("외부 주식 API 호출 (Phase 4)")
    }
}

@Component
class OrderEventKafkaAdapter(
    private val kafkaTemplate: KafkaTemplate<String, OrderCompletedEvent>,
) : PublishOrderEventPort {
    override fun publishOrderCompleted(event: OrderCompletedEvent) {
        kafkaTemplate.send("order.completed", event.orderId.toString(), event)
    }
}
```

Adapter 는 기술 세부사항을 담당. JPA, KafkaTemplate, RedisTemplate, WebClient, External API DTO, Entity Mapping 등은 Application/Domain 에 노출하지 않는다.

---

## Persistence Rules

핵심 도메인은 **Domain Model 과 JPA Entity 를 분리**한다.

```text
Domain Model: app.tick.order.domain.Order
JPA Entity:   app.tick.order.adapter.out.persistence.OrderJpaEntity
Mapper:       app.tick.order.adapter.out.persistence.OrderMapper
```

분리 이유:
- Domain 이 JPA annotation 에 오염되는 것 방지
- 테스트에서 DB 없이 도메인 로직 검증 가능
- Persistence 변경 시 Domain 영향 최소화

단순한 CRUD 도메인 (stock 등) 은 JPA Entity 를 Domain Model 로 사용 허용 (실제로 현재 `StockMaster` 가 그 케이스).

## Repository Rules

Application 은 Spring Data JPA Repository 에 직접 의존하지 않는다. Port 에 의존.

```kotlin
interface LoadOrderPort {
    fun loadById(orderId: Long): Order
}
interface SaveOrderPort {
    fun save(order: Order): Order
}

@Component
class OrderPersistenceAdapter(
    private val orderJpaRepository: OrderJpaRepository,
    private val orderMapper: OrderMapper,
) : LoadOrderPort, SaveOrderPort {
    override fun loadById(orderId: Long): Order {
        val entity = orderJpaRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException() }
        return orderMapper.toDomain(entity)
    }

    override fun save(order: Order): Order {
        val entity = orderMapper.toEntity(order)
        val saved = orderJpaRepository.save(entity)
        return orderMapper.toDomain(saved)
    }
}
```

---

## Flyway Migration Policy

DB 스키마 변경의 **단일 출처는 Flyway 마이그레이션 파일**이다.

### 규칙

- 위치: `backend/src/main/resources/db/migration/`
- 명명: `V<n>__<snake_case_description>.sql` (예: `V3__member.sql`)
- **이미 적용된 마이그레이션은 절대 수정 금지** (해시 검증 깨짐 → 부트 실패)
- 스키마 변경은 새 `V<n+1>` 로
- `spring.jpa.hibernate.ddl-auto: validate` 유지 — Hibernate 가 자동 ALTER 못 하게 막아 Flyway 단일 출처 보장
- `spring.flyway.baseline-on-migrate: true` — 빈 스키마에 첫 실행 시 baseline 처리

### 로컬 리셋

마이그레이션이 꼬여서 처음부터 다시 해야 할 때:

```bash
docker compose -f backend/compose.yaml down -v
docker compose -f backend/compose.yaml up -d
# 다음 bootRun 시 Flyway 가 V1, V2, ... 순차 적용
```

### `flyway-database-postgresql` 의존성 이유

Spring Boot 3.x + Flyway 10 에서는 Postgres 모듈이 분리됨. `flyway-database-postgresql` 가 없으면 `Unsupported Database: PostgreSQL` 에러.

### Identity 컬럼 정책

- `BIGSERIAL` 사용 (현재 V1/V2 일관). Postgres 16 이상에서는 `GENERATED ALWAYS AS IDENTITY` 가 모던 권장이나 Hibernate + Spring Data JPA 호환성 위해 BIGSERIAL 유지.
- 변경 시 일괄 변경 + 마이그레이션 추가 필요.

---

## API Rules

### 현재 (Phase 1-2)

REST API prefix: **`/api/*`**

```text
GET    /api/stocks
GET    /api/stocks/{symbol}
GET    /api/stocks/{symbol}/prices?days=N

GET    /api/account
POST   /api/account/deposit

GET    /api/portfolio
GET    /api/orders
GET    /api/transactions
```

### Phase 3 (카카오 인증 도입과 함께)

`/api/*` → **`/api/v1/*`** 일괄 전환 + 인증 엔드포인트 추가.

```text
POST   /api/v1/auth/kakao/callback   (백엔드 자동)
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/members/me

GET    /api/v1/stocks
GET    /api/v1/stocks/{stockCode}
GET    /api/v1/stocks/{stockCode}/prices

POST   /api/v1/orders/buy
POST   /api/v1/orders/sell
GET    /api/v1/orders
GET    /api/v1/orders/{orderId}
DELETE /api/v1/orders/{orderId}

GET    /api/v1/portfolio
GET    /api/v1/portfolio/holdings
GET    /api/v1/trades

GET    /api/v1/watchlist
POST   /api/v1/watchlist
DELETE /api/v1/watchlist/{stockCode}

GET    /api/v1/ai/stocks/{stockCode}/summary
```

전환 시 프론트 `src/services/*.ts` 의 fetch URL 도 동시 변경.

### Controller 규칙

- Request DTO → Command 변환
- UseCase 호출
- Result → Response DTO 변환
- **Entity / JPA Entity 직접 반환 금지**

---

## Common Response Format

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
) {
    companion object {
        fun <T> success(data: T) = ApiResponse(true, data, null)
        fun success() = ApiResponse(true, Unit, null)
        fun fail(message: String) = ApiResponse<Nothing>(false, null, message)
    }
}
```

> **현재 상태 주의**: 현재 컨트롤러들은 `ApiResponse` 로 감싸지 않고 raw object 를 반환 중. Phase 3 (`/api/v1` 전환) 와 함께 일괄 적용 예정. 그 전까지는 프론트 service 가 raw 응답 파싱.

---

## Frontend API Contract

백엔드 Response DTO 의 **필드명은 프론트 `src/types/*.ts` 와 일치**해야 한다. 변경 시 프론트도 동시 수정.

### 절대 경로

- `frontend/src/types/stock.ts` — `Stock`, `PricePoint`
- `frontend/src/types/portfolio.ts` — `Portfolio`, `Holding`
- `frontend/src/types/order.ts` — `Order`, `OrderStatus`, `OrderSide`, `OrderType`
- `frontend/src/types/account.ts` — `Account`, `Transaction`, `TransactionType`, `DailyAssetPoint`

### 인코딩 규칙

- 모든 필드는 **camelCase** (snake_case 안 됨)
- 금액: **정수 원 단위** (`Long`, JSON `number`)
- 비율: **Double** (`changeRate: 1.23` 은 1.23%)
- 시각: **ISO 8601 문자열** (`Instant.toString()` 결과)
- 날짜만: `YYYY-MM-DD` 문자열 (`LocalDate.toString()` 결과)
- nullable: 백엔드 `null` → 프론트 `null` (undefined 아님). 프론트 타입은 `field?: T | null` 형태 권장.
- 색상/표시 규칙(상승=빨강/하락=파랑)은 **프론트 책임**. 백엔드는 raw 숫자만 반환.

### 변경 절차

1. 백엔드 DTO 수정 → 빌드 통과
2. `frontend/src/types/*.ts` 동시 수정
3. `frontend/src/services/*.ts` 호출부 점검
4. 양쪽 build / lint 확인

---

## Order Domain Rules

### Buy Order

매수 규칙:
- 사용자 계좌 존재 필요
- 현재가 조회 성공 필요
- 보유 현금 ≥ 주문 금액
- 주문 금액 = `price * quantity`
- 체결 시 현금 차감
- 기존 Holding 있으면 평균 매입 단가 재계산, 없으면 Holding 생성
- Order → COMPLETED
- Trade 저장
- `OrderCompletedEvent` 발행

### Sell Order

매도 규칙:
- 사용자 계좌 존재 필요
- 해당 종목 Holding 존재 + 보유 수량 ≥ 매도 수량
- 체결 시 현금 증가
- Holding 수량 차감, 0 되면 closed 또는 삭제 정책
- **실현손익 계산**: `(price - holding.averagePrice) * quantity` → `Order.realizedProfitLoss` 에 기록
- Order → COMPLETED
- Trade 저장
- `OrderCompletedEvent` 발행

### Order Status / Type

```text
status:     PENDING, COMPLETED, CANCELED, FAILED
type/side:  BUY, SELL
orderType:  MARKET, LIMIT
```

---

## Account Domain Rules

- Account 는 Member 당 1개 (Phase 3 부터). 현재는 `external_id = 'demo'` 단일.
- 초기 가상 현금은 **자동 지급 아님**. 사용자가 명시적으로 `POST /api/account/deposit` 호출.
- 출금 시 잔고 부족 → 실패
- 입금 시 금액 > 0
- 모든 현금 변경은 `DepositHistory` (입출금) 또는 `OrderHistory` (주문 체결) 로 추적

도메인 메서드: `account.withdraw(order.totalAmount())`, `account.deposit(amount)`

---

## Holding Domain Rules

- 매수: 수량 증가 + 평균 매입 단가 재계산
- 매도: 수량 감소, 보유 < 매도 시 예외
- 수량 0 → closed 상태 또는 삭제 정책 (TBD)

도메인 메서드: `holding.applyBuy(order)`, `holding.applySell(order)`

---

## Portfolio Domain Rules

Portfolio 는 **조회 모델**. Aggregate 아님.

필요 데이터:
- 보유 종목 목록, 현재가, 평균 매입 단가, 보유 수량, 계좌 현금, 누적 실현손익

계산 항목:
- 총 매입금액 (`holdingsCost`)
- 총 평가금액 (`evaluationAmount`)
- 평가손익 (`unrealizedProfitLoss`)
- 실현손익 (`realizedProfitLoss`)
- 총 손익 (`totalProfitLoss = unrealized + realized`)
- 총 수익률 (`totalProfitRate = totalProfitLoss / totalDeposits * 100`)
- 오늘 손익 / 수익률
- 현금, 총 자산 (`cash + evaluationAmount`)

계산은 `PortfolioCalculator` Domain Service 로 분리 권장. 현재 `app.tick.account.PortfolioService` 에 인라인 구현 — Phase 3 hexagonal 전환 시 분리.

---

## Transaction Rules

Application Service 에서 transaction 관리. 하나의 transaction 으로 처리할 작업:

- 주문 생성
- 계좌 현금 차감/증가
- 보유 종목 생성/수정
- 체결 내역 저장
- 주문 상태 변경

### 주의

- **외부 API 호출은 transaction 시작 전** 또는 transaction 시간 최소화
- Kafka publish 는 **DB commit 이후** 발행 (초기엔 인라인, Phase 4 에서 Outbox)
- 동시성: 계좌 현금 차감, 보유 수량 변경, 중복 주문 방지 → 초기엔 DB pessimistic/optimistic lock. Redis lock 은 필요할 때만.

---

## 🚧 Phase 4 — Planned: 비동기 / 캐시 / AI

> 이하 섹션은 **현재 미적용**. Phase 4 에서 도입 시 본 규칙 적용. 지금 코드 작성 시 이 규칙을 미리 따르려고 노력할 필요 없음.

### Kafka (Phase 4)

실시간 시세 처리, 비동기 이벤트.

토픽 예시:
```text
stock.price.raw
stock.price.updated
order.created
order.completed
portfolio.updated
news.collected
ai.summary.requested
ai.summary.completed
```

```kotlin
data class StockPriceUpdatedEvent(
    val eventId: String,
    val stockCode: String,
    val price: Long,
    val changeRate: Double,
    val eventTime: Instant,
)
```

Consumer:
- 멱등성 필수 (같은 메시지 두 번 들어와도 데이터 깨지면 안 됨)
- `eventId` 또는 `aggregateId` 포함
- 실패 시 retry / DLQ
- Consumer 에서 복잡한 비즈니스 로직 직접 처리 X → Application Port In 호출

### Redis (Phase 4 일부 Phase 3)

용도:
- 실시간 주가 캐시
- JWT Refresh Token 저장 (Phase 3)
- 인기 종목 랭킹
- 짧은 TTL AI 요약 캐시
- 주문 중복 요청 방지
- WebSocket 세션 보조

Key 예시:
```text
stock:price:{stockCode}
auth:refresh:{memberId}
rank:popular-stocks
ai:summary:{stockCode}
order:dedupe:{memberId}:{requestId}
```

TTL 필수. Redis 접근은 반드시 Outbound Adapter 뒤.

### AI Summary (Phase 4)

주가 상승/하락 이유 보조 설명. **투자 권유 아님.**

#### 금지 표현

```text
매수해야 합니다
지금 사세요
확실히 오릅니다
수익이 보장됩니다
손실 가능성이 없습니다
```

#### 허용 표현

```text
상승 요인으로는 ...
시장에서는 ... 영향으로 해석됩니다
단기 변동성이 있을 수 있습니다
투자 판단은 사용자가 직접 해야 합니다
```

#### 응답 필수 문구

```text
본 요약은 정보 제공 목적이며 투자 권유가 아닙니다.
```

#### 구조

- AI Client 는 반드시 `AiSummaryPort` 뒤. 모델/프로바이더 변경에 도메인 영향 없도록.
- 프롬프트 입력: 도메인 인풋 (뉴스/공시/가격 이벤트) 만
- 응답 구조: `AiSummaryResult(summary: String, evidences: List<Evidence>)`
- 토큰 비용 추적은 어댑터 레벨
- LLM 응답 캐싱 (Redis): Phase 4

```kotlin
interface AiSummaryPort {
    fun summarize(command: AiSummaryCommand): AiSummaryResult
}
```

### WebSocket / SSE (Phase 4)

실시간 시세 push, 주문 체결 알림. WebSocket Handler 는 Inbound Adapter.

### News (Phase 4)

종목별 뉴스 수집 + 저장. 현재 frontend mock.

### Watchlist (Phase 3)

관심 종목. 단순 CRUD 라 hexagonal 강제 X, layered 로 충분.

---

## Security Rules

### Phase 3 도입 (카카오 OAuth + JWT)

- 인증: JWT 기반
- Access Token + Refresh Token 분리. Refresh 는 Redis 저장.
- 카카오 OAuth: `spring-boot-starter-oauth2-client`, redirect URI = `http://localhost:8080/login/oauth2/code/kakao`
- 시크릿: 환경변수 (`KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `JWT_SECRET`)
- Controller 에서 직접 토큰 파싱 X. `@AuthenticationPrincipal` 또는 custom resolver 로 주입.
- JWT Provider 는 Port 로 분리 가능
- 비밀번호 저장이 필요한 경우 (현재는 카카오 전용이라 불필요): BCrypt

### Phase 1-2 (현재)

인증 없음. 단일 demo 계정. 외부에 노출하지 않을 것 (CORS 가 `localhost:3000` 만 허용).

### Endpoint 보호 정책 (Phase 3)

```text
public:           /actuator/health, /login/oauth2/**, /oauth2/**
authenticated:    /api/v1/**
```

---

## Error Handling Rules

```kotlin
open class BusinessException(
    val errorCode: ErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)
```

```kotlin
enum class ErrorCode(val status: HttpStatus, val message: String) {
    // 도메인
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "보유 현금이 부족합니다."),
    INSUFFICIENT_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "보유 수량이 부족합니다."),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),

    // 인증 (Phase 3 도입 시 활성화)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    KAKAO_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "카카오 토큰이 유효하지 않습니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
}
```

모든 예외는 `@RestControllerAdvice` GlobalExceptionHandler 에서 처리. `ApiResponse.fail(...)` 형태 반환.

---

## Environment / Secrets

### 환경변수

| 키 | 용도 | 현재 상태 |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Postgres 접속 | `application.yml` 에 기본값 (`tick/tick/tick`) |
| `KAKAO_CLIENT_ID` | 카카오 OAuth | Phase 3 |
| `KAKAO_CLIENT_SECRET` | 카카오 OAuth | Phase 3 |
| `JWT_SECRET` | JWT 서명 | Phase 3 |
| `JWT_ACCESS_TTL_MIN` | access token TTL | Phase 3 |
| `JWT_REFRESH_TTL_DAYS` | refresh token TTL | Phase 3 |

### 프로필 분리

- `application.yml`: 로컬 개발 기본값 (절대 시크릿 X)
- `application-local.yml`: 로컬 오버라이드 (gitignore)
- `application-prod.yml`: 프로덕션 (gitignore)
- 시크릿은 환경변수 또는 secret manager 로 주입

### .gitignore 항목

```text
.env
.env.local
application-local.yml
application-prod.yml
**/secrets/
```

**시크릿을 git 에 커밋 금지.** 노출 시 카카오 콘솔에서 즉시 재발급.

---

## Time / Timezone

### 표현

- 도메인 시각: **`java.time.Instant`** (UTC 의미). DB `TIMESTAMP WITH TIME ZONE`.
- 날짜만: **`java.time.LocalDate`** (예: 일봉 시계열 날짜). Asia/Seoul 기준.
- API 응답: ISO 8601 문자열 (`Instant.toString()` → `2026-06-08T02:02:00Z`)
- 표시 (Asia/Seoul 변환) 은 **프론트 책임**

### 설정

`application.yml` 의 `spring.jpa.properties.hibernate.jdbc.time_zone: Asia/Seoul` — Hibernate 가 `TIMESTAMP WITH TIME ZONE` 컬럼을 Asia/Seoul 기준 Instant 로 매핑. DB 자체는 UTC 로 저장.

### 가격 시계열 날짜

`StockPriceGenerator` 가 `LocalDate.now(ZoneId.of("Asia/Seoul"))` 사용 — 장 마감일 단위. 프론트의 mulberry32 결과와 동일한 날짜 시퀀스 보장.

---

## CORS

현재 설정 (`CorsConfig.kt`):

- 매핑: `/api/**` 만
- 허용 origin: `tick.cors.allowed-origins` (콤마 분리)
- 기본값: `http://localhost:3000`
- `allowCredentials: true` (Phase 3 카카오 인증 위해 쿠키 전송 가능해야 함)

### Phase 3 주의

- `allowCredentials: true` + wildcard origin (`*`) 조합 금지 (브라우저가 막음). 명시적 origin 만.
- 프로덕션: `https://tick.example.com` 등 정확히 명시

### `/actuator/**`

현재 `/api/**` 만 CORS 매핑. `/actuator` 는 사내 모니터링용이라 외부에서 호출 X. Phase 3 에서 Spring Security 로 IP 제한 또는 인증 필요.

---

## Logging

- SLF4J. Kotlin 에선 `org.slf4j.LoggerFactory` 직접 또는 `KotlinLogging` (선택).
- 레벨:
  - **ERROR**: 예상치 못한 예외 (재시도 불가, 알림 필요)
  - **WARN**: 복구 가능한 이상 상황 (외부 API 일시 실패, 재시도 진행)
  - **INFO**: 상태 변경 (주문 체결, 입금, 로그인)
  - **DEBUG**: 상세 흐름 (port 호출 인자 등). 로컬 전용.
- PII 금지: 이메일은 마스킹 (`a***@kakao.com`), JWT/Refresh Token/시크릿은 절대 출력 X
- 구조화: SLF4J placeholder 패턴 사용 (`logger.info("order completed orderId={} memberId={}", orderId, memberId)`) — 문자열 concat 금지

---

## Observability

### 현재

- Actuator: `/actuator/health`, `/actuator/info`
- `application.yml`:
  ```yaml
  management.endpoints.web.exposure.include: health,info
  ```

### Phase 4

- Micrometer + Prometheus (메트릭)
- OpenTelemetry (분산 트레이싱)
- 구조화 로그 → Loki / CloudWatch / Datadog

---

## Kotlin Coding Rules

- `val` 우선. `var` 는 필요할 때만.
- nullable 타입은 명확한 이유가 있을 때만.
- **`!!` 사용 금지.** non-null 보장은 require/check/elvis 로.
- DTO 는 `data class`.
- Domain Model 은 가능한 한 framework annotation 없이.
- JPA Entity 는 `adapter.out.persistence` (또는 단순 도메인은 도메인 패키지 내) 에 둔다.
- 비즈니스 로직은 Controller 에 작성 X.
- 의미 없는 `Manager`, `Util`, `Helper` 남발 X.
- public setter 남발 X. `var` 는 도메인 메서드에서만 갱신.
- Boolean 반환 메서드는 의미가 드러나게: `isCompleted()`, `hasEnoughCash()` ✅ vs `check()`, `validate()` ❌
- 메서드는 하나의 의도. 복잡한 조건식은 의미 있는 메서드로 분리.
- value class (`@JvmInline`) 적극 사용: VO 의 런타임 오버헤드 줄임.

---

## Test Rules

### 우선순위

1. Value Object 테스트 (Money, Quantity, ProfitLoss)
2. Account 출금/입금 도메인 테스트
3. Holding 평균 매입 단가 계산 테스트
4. 매수 주문 유스케이스 테스트
5. 매도 주문 유스케이스 테스트
6. 포트폴리오 평가금액 계산 테스트
7. Kafka Consumer 멱등성 테스트 (Phase 4)
8. Redis TTL/cache 테스트 (Phase 4)
9. AI Summary Port Mock 테스트 (Phase 4)

### 스타일

- 테스트 이름 한글 허용:
  ```kotlin
  @Test
  fun `현금 잔고가 부족하면 매수 주문에 실패한다`() { /* ... */ }
  ```
- 외부 API 는 테스트에서 실제 호출 X. Outbound Port 를 mock 또는 fake 로 대체.
- Application Service 테스트에서 검증: 올바른 Port 호출, 도메인 상태 변경, 예외 처리, 이벤트 발행 요청.
- Phase 3 부터 Testcontainers 도입 — `@Testcontainers` + Postgres 컨테이너로 통합 테스트.

---

## Database Rules

- 금액: `BIGINT` (Long). **Float/Double 금지.** 1 = 1원.
- 수익률 / 등락률: 비율은 `DOUBLE` 또는 `NUMERIC` (현재 Double, 표시용으로 충분).
- `created_at`, `updated_at` 은 모든 주요 테이블에 포함.
- Soft delete 필요한 도메인은 `deleted_at` 추가.
- 주문 / 체결 / 계좌 변경 이력은 감사 가능성 위해 **삭제하지 않는다**.
- 주요 FK 에 index.
- 주문 조회, 체결 내역 조회, 보유 종목 조회에는 조회 성능 index.

자세한 정책은 위 **Flyway Migration Policy** 참고.

---

## Naming Rules

### Port In (UseCase)

```text
CreateOrderUseCase, CancelOrderUseCase, GetOrderUseCase,
GetPortfolioUseCase, GetCurrentStockPriceUseCase, SummarizeStockReasonUseCase
```

### Port Out

```text
LoadAccountPort, SaveAccountPort, LoadHoldingPort, SaveHoldingPort,
LoadOrderPort, SaveOrderPort, SaveTradePort,
LoadStockPricePort, PublishOrderEventPort, CacheStockPricePort,
LoadAiSummaryPort, SaveAiSummaryPort
```

### Adapter

```text
OrderPersistenceAdapter, AccountPersistenceAdapter, HoldingPersistenceAdapter,
StockPriceApiAdapter, OrderEventKafkaAdapter, StockPriceRedisAdapter,
OpenAiSummaryAdapter
```

### Command

```text
CreateBuyOrderCommand, CreateSellOrderCommand, CancelOrderCommand, SummarizeStockReasonCommand
```

### Result

```text
CreateOrderResult, PortfolioResult, StockPriceResult, AiSummaryResult
```

### Domain Method

```text
withdraw(), deposit(), applyBuy(), applySell(),
complete(), cancel(), fail(), totalAmount(), calculateProfitRate()
```

---

## Prohibited Actions

Claude 가 하면 안 되는 것:

- Controller 에 비즈니스 로직 작성
- Domain 에서 Spring Bean, JPA Repository, KafkaTemplate, RedisTemplate, WebClient 사용
- Domain Model 에 JPA annotation 추가
- Application Service 가 WebClient, KafkaTemplate, RedisTemplate 에 직접 의존
- 모든 기능에 기계적으로 Port/UseCase/Adapter 생성
- 단순 CRUD 에 과도한 DDD 구조 적용
- Entity 에 무분별한 setter 추가
- 모든 필드 nullable 화
- 테스트 없는 핵심 주문 로직 작성
- 외부 API 키를 코드에 하드코딩
- DB transaction 안에서 오래 걸리는 외부 API 호출
- 투자 권유성 AI 답변 생성
- 실제 주식 주문 API 호출
- 사용하지 않는 추상화 남발
- 과도한 MSA 구조 도입
- Application Service 에 평균 단가 계산, 잔고 검증, 보유 수량 검증 같은 세부 도메인 규칙을 길게 작성
- **이미 적용된 Flyway 마이그레이션 파일 수정** (해시 깨짐)
- **`com.tick.backend` 패키지 사용** (실제는 `app.tick`)
- 시크릿 git 커밋

---

## Development Priority

### 완료 (Phase 1-2)

1. ✅ 프로젝트 기본 세팅
2. ✅ Postgres + Flyway + Stock master 시드
3. ✅ Stock 목록 / 단건 / 시세 조회 API
4. ✅ Account / Holding / OrderHistory / DepositHistory 스키마
5. ✅ Account / Portfolio / Orders / Transactions 조회 API
6. ✅ Deposit POST API
7. ✅ CORS for `localhost:3000`

### 진행 중 (Phase 2 마무리)

8. POST 주문 (매수/매도) 유스케이스 + 실현손익 자동 계산
9. 도메인 분리 시작 — Money/Quantity/ProfitLoss VO 도입, `app.tick.account` → hexagonal 전환

### 다음 (Phase 3)

10. Member 도메인 + `users` 테이블 (V3 마이그레이션)
11. Spring Security + 카카오 OAuth + JWT
12. `account.member_id` FK, `external_id = 'demo'` 폐기
13. `/api/*` → `/api/v1/*` 일괄 전환 + 프론트 service 수정
14. Watchlist
15. Redis (refresh token, 시세 캐시)

### 미래 (Phase 4)

16. Kafka (실시간 시세 이벤트, AI 비동기 트리거)
17. WebSocket / SSE 실시간 가격 전송
18. News 수집
19. AI 상승/하락 이유 요약 (LLM port + adapter)
20. 인기 종목 랭킹
21. pgvector RAG
22. Observability (메트릭 / 트레이싱)

---

## Hexagonal Migration Path

현재 layered 코드를 도메인별로 점진 전환. 한 번에 다 안 함.

### Step 1: `account` 도메인 hexagonal 전환

- `domain/` 폴더 생성 — Account / Holding / OrderHistory / DepositHistory 를 도메인 모델로 (JPA 어노테이션 제거)
- VO 도입: `Money`, `Quantity`, `ProfitLoss`, `StockCode`
- `application/port/in/` — `GetAccountUseCase`, `DepositUseCase`, `GetPortfolioUseCase`, `GetOrdersUseCase`, `GetTransactionsUseCase`
- `application/port/out/` — `LoadAccountPort`, `SaveAccountPort`, `LoadHoldingsPort`, `LoadOrderHistoryPort`, `LoadDepositHistoryPort`, `SaveDepositHistoryPort`
- `application/service/` — 기존 AccountService / PortfolioService / OrderHistoryService / TransactionService 를 UseCase 구현체로 리팩토링
- `adapter/in/web/` — 기존 Controllers.kt 분할
- `adapter/out/persistence/` — JPA Entity (`AccountJpaEntity` 등) + Repository + Mapper + PersistenceAdapter
- API contract 동일 유지 (프론트 코드 변경 X)
- 테스트 통과 확인

### Step 2: 매수/매도 POST 주문 추가 + `order` 도메인 분리

- `app.tick.order` 패키지 신설 (account 에서 OrderHistory 이동)
- `CreateOrderUseCase` (buy/sell)
- `OrderHistory` → 도메인 `Order` + `OrderJpaEntity` 분리
- 실현손익 자동 계산 (`AveragePriceCalculator` Domain Service)

### Step 3: 카카오 OAuth + `member` / `auth` 도메인 신설

- `member` 도메인은 처음부터 hexagonal 로
- `auth` 는 Spring Security 통합이라 layered + 분리

### Step 4 이후: 나머지 도메인

- `stock` 은 layered 유지 가능 (단순 조회). 실시간 시세 API 도입 시 어댑터 분리.
- `watchlist` 는 단순 CRUD, layered 충분.
- `ai`, `news`, `market` 는 처음부터 hexagonal (외부 의존 多).

각 step 마다 **API contract 동일 유지 + 동작 테스트 통과** 가 합격선.

---

## Claude Response Rule

Claude 는 코드 작성 시:

- 변경한 파일 목록을 먼저 요약
- 어떤 Domain Model, VO, Port, Adapter 를 만들었는지 설명
- 핵심 유스케이스 흐름 설명
- Application Service 만 봐도 흐름이 읽히는지 점검
- 필요 시 실행 명령어 제공
- 핵심 비즈니스 로직에는 테스트 코드 함께 작성
- 불확실한 외부 API 스펙은 임의로 단정 X → TODO 또는 interface 분리
- 기존 아키텍처 규칙을 깨는 변경은 먼저 이유 설명
- **현재 코드 (Phase 1-2 layered) 를 수정할 때는 hexagonal 마이그레이션 단계인지, 단순 패치인지 명시**

---

## Important Business Constraints

이 서비스는 **실제 투자 서비스가 아니라 모의투자 서비스**다.

- 실제 주문 체결 API 호출 X
- 실제 금융투자 권유 문구 X
- 사용자의 가상 자산만 변경
- AI 요약은 정보 제공 목적
- 실시간 주가는 외부 API 또는 Kafka Stream 통해 반영 (Phase 4)
- 외부 API 장애 시에도 캐시 데이터로 최소 조회 가능 (Phase 4)
- 모든 금액 / 수량 변경은 추적 가능
- 주문 / 체결 / 현금 변경 이력은 감사 가능성 위해 보존
