package app.tick.account.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "account")
class AccountJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "external_id", unique = true)
    val externalId: String,
    @Column(name = "member_id")
    val memberId: Long? = null,
    var cash: Long,
    @Column(name = "total_deposits")
    var totalDeposits: Long,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "updated_at")
    var updatedAt: Instant,
)

@Entity
@Table(name = "holding")
class HoldingJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "account_id")
    val accountId: Long,
    val symbol: String,
    var quantity: Int,
    @Column(name = "average_price")
    var averagePrice: Int,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "updated_at")
    var updatedAt: Instant,
)

@Entity
@Table(name = "deposit_history")
class DepositHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "account_id")
    val accountId: Long,
    val amount: Long,
    val type: String,
    @Column(name = "created_at")
    val createdAt: Instant,
)
