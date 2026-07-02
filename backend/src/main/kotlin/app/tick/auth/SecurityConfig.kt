package app.tick.auth

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableConfigurationProperties(JwtProperties::class, AuthProperties::class)
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val kakaoOAuth2UserService: KakaoOAuth2UserService,
    private val oauth2SuccessHandler: OAuth2SuccessHandler,
    private val oauth2FailureHandler: OAuth2FailureHandler,
    private val authorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { } // CorsConfig 의 WebMvcConfigurer 가 처리
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/login/**",
                        "/oauth2/**",
                        "/api/v1/auth/**",
                        "/api/v1/stocks",
                        "/api/v1/stocks/**",
                        // /ws/market: 실시간 시세는 유저별 데이터 아니라 public 허용.
                        // 프론트가 종목 상세 페이지에서 로그인 없이도 시세를 봐야 함.
                        "/ws/market",
                    ).permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/news/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth ->
                oauth
                    .authorizationEndpoint {
                        it.authorizationRequestRepository(authorizationRequestRepository)
                    }
                    .userInfoEndpoint { it.userService(kakaoOAuth2UserService) }
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )
        return http.build()
    }
}
