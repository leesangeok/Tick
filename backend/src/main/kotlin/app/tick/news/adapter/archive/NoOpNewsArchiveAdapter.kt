package app.tick.news.adapter.archive

import app.tick.news.application.NewsArchivePort
import app.tick.news.domain.News
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 아카이빙 비활성 (`tick.news.archive.enabled=false` — 기본) 또는 s3 bucket 미설정 시 사용.
 * archive_url 은 null 로 남고 news 는 기존 흐름대로 저장.
 */
@Component
@ConditionalOnProperty(
    prefix = "tick.news.archive",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class NoOpNewsArchiveAdapter : NewsArchivePort {
    override fun archive(news: News): String? = null
}
