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
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth ->
                oauth
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
