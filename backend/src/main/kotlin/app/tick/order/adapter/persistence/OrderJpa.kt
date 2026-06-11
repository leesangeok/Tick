package app.tick.order.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

@Entity
@Table(name = "order_history")
class OrderJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "account_id")
    val accountId: Long,
    val symbol: String,
    val side: String,
    @Column(name = "order_type")
    val orderType: String,
    val quantity: Int,
    val price: Int,
    @Column(name = "filled_quantity")
    val filledQuantity: Int?,
    val status: String,
    @Column(name = "average_cost_at")
    val averageCostAt: Int?,
    @Column(name = "realized_profit_loss")
    val realizedProfitLoss: Long?,
    @Column(name = "created_at")
    val createdAt: Instant,
    @Column(name = "filled_at")
    val filledAt: Instant?,
)

interface OrderJpaRepository : JpaRepository<OrderJpaEntity, Long> {
    fun findAllByAccountIdOrderByCreatedAtDesc(accountId: Long): List<OrderJpaEntity>
}
