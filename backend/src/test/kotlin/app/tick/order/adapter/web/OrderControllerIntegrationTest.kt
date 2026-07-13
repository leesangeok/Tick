package app.tick.order.adapter.web

import app.tick.support.IntegrationTestBase
import app.tick.support.TestDataSeeder
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * OrderController 통합 테스트 — 컨트롤러 계층의 auth / validation 경로 검증.
 * 실주문 흐름 (계좌/보유/시세) 은 OrderServiceTest 의 unit test 로 커버.
 */
class OrderControllerIntegrationTest : IntegrationTestBase() {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var seeder: TestDataSeeder

    private fun body(quantity: Int = 1, orderType: String = "MARKET") = objectMapper.writeValueAsString(
        mapOf("stockCode" to "005930", "quantity" to quantity, "orderType" to orderType),
    )

    @Test
    fun `주문 API 는 JWT 없이 호출하면 401`() {
        mockMvc.perform(
            post("/api/v1/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `수량이 0 이면 400 INVALID_ORDER_QUANTITY`() {
        mockMvc.perform(
            post("/api/v1/orders/buy")
                .header(HttpHeaders.AUTHORIZATION, bearer(memberId = 9001L))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(quantity = 0)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_QUANTITY"))
    }

    @Test
    fun `주문 유형이 잘못되면 400 INVALID_ORDER_TYPE`() {
        mockMvc.perform(
            post("/api/v1/orders/buy")
                .header(HttpHeaders.AUTHORIZATION, bearer(memberId = 9002L))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(orderType = "SWEEP")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_TYPE"))
    }

    @Test
    fun `계좌가 시드된 사용자는 주문 목록을 조회하고 초기엔 빈 배열이다`() {
        val memberId = seeder.insertMember(kakaoId = 2_000_001L)
        seeder.insertAccount(memberId)

        mockMvc.perform(
            get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(memberId)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))
    }
}
