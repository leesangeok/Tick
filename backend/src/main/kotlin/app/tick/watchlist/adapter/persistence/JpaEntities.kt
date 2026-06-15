package app.tick.watchlist.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "watchlist",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "symbol"])],
)
class WatchlistJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "member_id")
    val memberId: Long,
    val symbol: String,
    @Column(name = "created_at")
    val createdAt: Instant,
)
