package app.tick.account.adapter.web

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

class AccountControllerIntegrationTest : IntegrationTestBase() {
    @Autowired
    lateinit var seeder: TestDataSeeder

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `계좌 조회는 JWT 없이 호출하면 401`() {
        mockMvc.perform(get("/api/v1/account"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `계좌가 없는 사용자는 404 ACCOUNT_NOT_FOUND`() {
        val memberId = seeder.insertMember(kakaoId = 1_000_001L)
        mockMvc.perform(
            get("/api/v1/account")
                .header(HttpHeaders.AUTHORIZATION, bearer(memberId)),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
    }

    @Test
    fun `계좌가 시드된 사용자는 200 으로 계좌 정보를 반환한다`() {
        val memberId = seeder.insertMember(kakaoId = 1_000_002L)
        seeder.insertAccount(memberId, cash = 5_000_000L, totalDeposits = 5_000_000L)

        mockMvc.perform(
            get("/api/v1/account")
                .header(HttpHeaders.AUTHORIZATION, bearer(memberId)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.cash").value(5_000_000))
            .andExpect(jsonPath("$.data.totalDeposits").value(5_000_000))
    }

    @Test
    fun `입금 요청 시 잔액과 총 입금액이 증가한다`() {
        val memberId = seeder.insertMember(kakaoId = 1_000_003L)
        seeder.insertAccount(memberId, cash = 1_000_000L, totalDeposits = 1_000_000L)

        val body = objectMapper.writeValueAsString(mapOf("amount" to 500_000L))
        mockMvc.perform(
            post("/api/v1/account/deposit")
                .header(HttpHeaders.AUTHORIZATION, bearer(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.cash").value(1_500_000))
            .andExpect(jsonPath("$.data.totalDeposits").value(1_500_000))
    }
}
