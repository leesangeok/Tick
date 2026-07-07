package app.tick.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.ConfigurationCondition
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.RedisMessageListenerContainer

/**
 * Redis Pub/Sub 리스너 컨테이너. broadcast 어댑터가 message subscriber 로 등록해서 다른
 * backend 인스턴스가 publish 한 메시지를 수신한다.
 *
 * broadcast mode 가 하나라도 redis 이면 활성화 (market 또는 orders).
 */
@Configuration
@Conditional(AnyBroadcastRedisCondition::class)
class RedisPubSubConfig {

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        return container
    }
}

/** market 또는 orders 어느 한쪽이 redis 모드이면 true. */
class AnyBroadcastRedisCondition : ConfigurationCondition {
    override fun getConfigurationPhase(): ConfigurationPhase = ConfigurationPhase.REGISTER_BEAN

    override fun matches(context: org.springframework.context.annotation.ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        val market = env.getProperty("tick.market.broadcast.mode", "inprocess")
        val orders = env.getProperty("tick.orders.broadcast.mode", "inprocess")
        return market == "redis" || orders == "redis"
    }
}
