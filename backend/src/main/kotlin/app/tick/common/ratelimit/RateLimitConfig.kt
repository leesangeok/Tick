package app.tick.common.ratelimit

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// RateLimitInterceptor 를 `/api/` 아래 경로에 적용. WS handshake, actuator, static 은 제외.
// auth 는 콜백에서 자체 폭주 방지 있으니 제외.
@Configuration
class RateLimitConfig(
    private val interceptor: RateLimitInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/v1/auth/**")
    }
}
