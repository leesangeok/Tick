package app.tick.news.domain

import app.tick.common.domain.StockCode
import java.security.MessageDigest
import java.time.Instant

data class News(
    val id: Long?,
    val stockCode: StockCode,
    val title: String,
    val body: String,
    val source: String?,
    val sourceUrl: String?,
    val publishedAt: Instant,
    val contentHash: String,
    // 원본 body 를 S3 에 아카이빙한 URL (nullable — 아카이빙 비활성/실패 시 null).
    val archiveUrl: String? = null,
) {
    fun withArchiveUrl(url: String?): News = copy(archiveUrl = url)

    companion object {
        fun newOne(
            stockCode: StockCode,
            title: String,
            body: String,
            source: String?,
            sourceUrl: String?,
            publishedAt: Instant,
        ): News = News(
            id = null,
            stockCode = stockCode,
            title = title,
            body = body,
            source = source,
            sourceUrl = sourceUrl,
            publishedAt = publishedAt,
            contentHash = hashOf(title, body),
        )

        fun hashOf(title: String, body: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest("$title\n$body".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
