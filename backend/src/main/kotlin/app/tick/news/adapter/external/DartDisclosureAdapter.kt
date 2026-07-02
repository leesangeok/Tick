package app.tick.news.adapter.external

import app.tick.common.domain.StockCode
import app.tick.news.application.NewsCollectorPort
import app.tick.news.domain.News
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Configuration
@EnableConfigurationProperties(DartProperties::class)
class DartConfig

/**
 * opendart.fss.or.kr `list.json` 을 호출해 종목별 최근 공시를 News 로 변환하는 collector.
 *
 * enabled=false 또는 api-key 미설정 시 즉시 emptyList → 다른 collector (네이버 등) 는 정상 동작.
 * corp_code 매핑에 없는 종목이면 skip. include-types (기본 A/B/C) 에 해당하는 공시만 통과.
 *
 * body 는 초기 iteration 에선 report_nm + 제출인 + 유형명 요약만. 본문 원문 파싱 (rcpNo 별 XML)
 * 은 후속 작업 — 지금은 hallucination 방지 목적에 report_nm 만으로도 유의미하다.
 */
@Component
class DartDisclosureAdapter(
    private val properties: DartProperties,
    private val corpCodeMap: DartCorpCodeMap,
) : NewsCollectorPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client: RestClient = RestClient.create()
    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val kst = ZoneId.of("Asia/Seoul")

    override fun search(stockCode: StockCode, limit: Int): List<News> {
        if (!properties.enabled) return emptyList()
        if (properties.apiKey.isBlank()) return emptyList()

        val corpCode = corpCodeMap.lookup(stockCode) ?: run {
            log.debug("DART corp_code miss for {}", stockCode.value)
            return emptyList()
        }
        val today = LocalDate.now(kst)
        val since = today.minusDays(properties.lookbackDays.toLong())

        val response = try {
            client.get()
                .uri {
                    it.scheme("https").host("opendart.fss.or.kr").path("/api/list.json")
                        .queryParam("crtfc_key", properties.apiKey)
                        .queryParam("corp_code", corpCode)
                        .queryParam("bgn_de", since.format(dateFmt))
                        .queryParam("end_de", today.format(dateFmt))
                        .queryParam("page_no", 1)
                        .queryParam("page_count", properties.pageCount)
                        .build()
                }
                .retrieve()
                .body<DartListResponse>()
        } catch (e: Exception) {
            log.warn("DART call failed for {}: {}", stockCode.value, e.message)
            return emptyList()
        } ?: return emptyList()

        // 013 = 조회된 데이터 없음. 정상 무 이벤트 케이스.
        if (response.status == "013") return emptyList()
        if (response.status != "000") {
            log.warn("DART non-ok status={} msg={}", response.status, response.message)
            return emptyList()
        }

        val kept = response.list.orEmpty()
            .filter { it.pblntfTy in properties.includeTypes }
            .take(limit)

        return kept.mapNotNull { item ->
            try {
                News.newOne(
                    stockCode = stockCode,
                    title = "[공시] ${item.reportNm.trim()}",
                    body = buildBody(item),
                    source = "DART",
                    sourceUrl = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=${item.rceptNo}",
                    publishedAt = parseRceptDt(item.rceptDt),
                )
            } catch (e: Exception) {
                log.warn("skip malformed DART item {}: {}", item.rceptNo, e.message)
                null
            }
        }
    }

    private fun buildBody(item: DartListItem): String = buildString {
        append("공시제목: ").append(item.reportNm.trim()).append('\n')
        append("제출인: ").append(item.flrNm ?: "-").append('\n')
        append("공시유형: ").append(disclosureTypeName(item.pblntfTy))
        item.rm?.takeIf { it.isNotBlank() }?.let { append('\n').append("비고: ").append(it) }
    }

    private fun disclosureTypeName(ty: String?): String = when (ty) {
        "A" -> "정기공시"
        "B" -> "주요사항보고"
        "C" -> "발행공시"
        "D" -> "지분공시"
        "E" -> "기타공시"
        else -> "공시"
    }

    private fun parseRceptDt(raw: String): Instant {
        // "20260702" 형식. list.json 에는 시각 필드가 없어 15:00 KST (장 마감 근처) 로 근사.
        val d = LocalDate.parse(raw, dateFmt)
        return d.atTime(15, 0).atZone(kst).toInstant()
    }
}

private data class DartListResponse(
    val status: String = "",
    val message: String? = null,
    val list: List<DartListItem>? = null,
)

private data class DartListItem(
    @JsonProperty("rcept_no") val rceptNo: String,
    @JsonProperty("corp_code") val corpCode: String,
    @JsonProperty("corp_name") val corpName: String,
    @JsonProperty("report_nm") val reportNm: String,
    @JsonProperty("rcept_dt") val rceptDt: String,
    @JsonProperty("flr_nm") val flrNm: String? = null,
    @JsonProperty("rm") val rm: String? = null,
    @JsonProperty("pblntf_ty") val pblntfTy: String? = null,
)
