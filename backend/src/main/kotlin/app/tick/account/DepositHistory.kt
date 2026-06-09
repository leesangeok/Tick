package app.tick.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "deposit_history")
class DepositHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "account_id")
    val accountId: Long,
    val amount: Long,
    val type: String = "DEPOSIT",
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
