package app.tick.stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "stock_master")
class StockMaster(
    @Id
    val symbol: String,
    val name: String,
    val market: String,
    val sector: String,
    @Column(name = "base_price")
    val basePrice: Int,
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
