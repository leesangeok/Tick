package app.tick.news.adapter.external

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tick.naver.news")
data class NaverNewsProperties(
    val clientId: String = "",
    val clientSecret: String = "",
    val baseUrl: String = "https://openapi.naver.com/v1/search/news.json",
)
