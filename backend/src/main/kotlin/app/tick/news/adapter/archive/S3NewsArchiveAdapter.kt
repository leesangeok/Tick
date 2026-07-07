package app.tick.news.adapter.archive

import app.tick.news.application.NewsArchivePort
import app.tick.news.domain.News
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * S3 로 원본 body 업로드. key 는 `news/{yyyy}/{MM}/{dd}/{symbol}/{contentHash}.txt`.
 * 실패 시 null 반환 — 뉴스 저장 자체는 계속 진행 (아카이빙은 보조 기능).
 *
 * 인증은 AWS 기본 credentials chain — EC2 instance profile (배포용) 또는 로컬 개발 env 변수.
 */
@Component
@ConditionalOnProperty(
    prefix = "tick.news.archive",
    name = ["enabled"],
    havingValue = "true",
)
class S3NewsArchiveAdapter(
    @Value("\${tick.news.archive.s3-bucket}") private val bucket: String,
    @Value("\${tick.news.archive.s3-region:ap-northeast-2}") private val region: String,
) : NewsArchivePort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("UTC"))

    private val s3: S3Client by lazy {
        // lazy init — bean 생성 시점에 AWS 인증 시도하지 않도록. 로컬 dev 에서 인증 미설정이어도
        // 어댑터 자체 인스턴스는 만들어져 있어야 InProcess 브랜치가 정상 동작.
        S3Client.builder().region(Region.of(region)).build()
    }

    override fun archive(news: News): String? {
        val key = "news/${dateFormatter.format(news.publishedAt)}/${news.stockCode.value}/${news.contentHash}.txt"
        return try {
            s3.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("text/plain; charset=utf-8")
                    .build(),
                RequestBody.fromString(news.body),
            )
            "s3://$bucket/$key"
        } catch (e: Exception) {
            log.warn("s3 archive failed hash={} err={}", news.contentHash, e.message)
            null
        }
    }
}
