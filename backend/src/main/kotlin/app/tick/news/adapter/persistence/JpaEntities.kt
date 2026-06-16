package app.tick.news.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "news")
class NewsJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val symbol: String,
    val title: String,
    val body: String,
    val source: String? = null,
    @Column(name = "source_url")
    val sourceUrl: String? = null,
    @Column(name = "published_at")
    val publishedAt: Instant,
    @Column(name = "content_hash", length = 64, unique = true)
    val contentHash: String,
    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
