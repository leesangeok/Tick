package app.tick.news.adapter.external

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tick.dart")
data class DartProperties(
    /** false 면 어댑터가 no-op — 다른 collector 는 정상 동작. */
    val enabled: Boolean = false,
    /** opendart.fss.or.kr 발급 키. blank 면 어댑터가 no-op. */
    val apiKey: String = "",
    val baseUrl: String = "https://opendart.fss.or.kr/api",
    /** 각 종목당 조회할 lookback 창 (일). */
    val lookbackDays: Int = 30,
    /** page_count 파라미터. DART 는 최대 100. */
    val pageCount: Int = 100,
    /**
     * 관심 공시 유형. A 정기공시 / B 주요사항 / C 발행공시 / D 지분 / E 기타.
     * 기본은 가격 파급 큰 상위 세 유형만. D/E 는 노이즈 커서 제외.
     */
    val includeTypes: List<String> = listOf("A", "B", "C"),
)
