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
    /**
     * report_nm 에 포함되면 요약 근거 후보에서 제외하는 소음 키워드.
     * eval 관측: SK하이닉스·셀트리온 등에서 "임원ㆍ주요주주특정증권등소유상황보고서" 류가
     * top-K 에 유입되며 요약을 오염시키는 사례 다수. 회사가 발표한 실적/증자/M&A 공시만
     * 근거로 남기고 지분 신고류는 컷.
     */
    val noiseTitleKeywords: List<String> = listOf(
        "임원",
        "주요주주",
        "특정증권",
        "소유상황",
    ),
)
