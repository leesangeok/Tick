package app.tick.support

import app.tick.TestcontainersConfiguration
import app.tick.auth.JwtProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc

/**
 * Controller 통합 테스트 공통 세팅.
 * - Testcontainers Postgres + Redis
 * - MockMvc 로 실제 SecurityFilterChain 통과
 * - JwtProvider 를 노출해 실제 JWT 를 발급 → JwtAuthenticationFilter 를 실제로 태운다.
 *
 * OAuth2 client / 외부 API 설정은 시크릿이 없어도 컨텍스트 로딩만 되도록 더미값 주입.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@TestPropertySource(
    properties = [
        "tick.jwt.secret=integration-test-secret-must-be-long-enough-for-hmac-sha-256-signing-key",
        "tick.jwt.access-ttl-min=60",
        "tick.jwt.refresh-ttl-days=14",
        "tick.naver.news.client-id=test",
        "tick.naver.news.client-secret=test",
        "tick.dart.enabled=false",
        "tick.market.enabled=false",
        "tick.ai-server.url=http://localhost:65535",
        "spring.security.oauth2.client.registration.kakao.client-id=test",
        "spring.security.oauth2.client.registration.kakao.client-secret=test",
    ],
)
abstract class IntegrationTestBase {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jwtProvider: JwtProvider

    fun bearer(memberId: Long, nickname: String? = "integration-test"): String =
        "Bearer " + jwtProvider.issueAccessToken(memberId, nickname)
}
