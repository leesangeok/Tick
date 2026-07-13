package app.tick.news.adapter.web

import app.tick.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * News 조회는 permitAll, 수집(POST) 은 인증 필요.
 * DART / Naver 어댑터는 dummy key 로 disabled, 실제 외부 호출은 발생하지 않음.
 */
class NewsControllerIntegrationTest : IntegrationTestBase() {

    @Test
    fun `최근 뉴스 조회는 공개 API 이며 없는 종목은 빈 배열`() {
        mockMvc.perform(get("/api/v1/news/999999"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `뉴스 수집 POST 는 JWT 없이 호출하면 401`() {
        mockMvc.perform(post("/api/v1/news/005930/collect"))
            .andExpect(status().isUnauthorized)
    }
}
