package io.clroot.ball.adapter.outbound.data.access.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.integration.redis.util.RedisLockRegistry

@Configuration
@EnableRedisRepositories
class RedisConfig(
    @param:Value($$"${spring.data.redis.host}")
    private val redisHost: String,
    @param:Value($$"${spring.data.redis.port}")
    private val redisPort: Int,
    @param:Value($$"${spring.data.redis.password:}")
    private val redisPassword: String,
) {
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val redisConfig =
            RedisStandaloneConfiguration().apply {
                hostName = redisHost
                port = redisPort
                if (redisPassword.isNotBlank()) {
                    setPassword(redisPassword)
                }
            }
        return LettuceConnectionFactory(redisConfig)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            connectionFactory = redisConnectionFactory()

            // 직렬화 설정
            keySerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            valueSerializer = JacksonJsonRedisSerializer(Any::class.java)
            hashValueSerializer = JacksonJsonRedisSerializer(Any::class.java)

            afterPropertiesSet()
        }

    @Bean
    fun stringRedisTemplate(): StringRedisTemplate =
        StringRedisTemplate().apply {
            connectionFactory = redisConnectionFactory()
        }

    @Bean
    fun redisLockRegistry(redisConnectionFactory: RedisConnectionFactory): RedisLockRegistry =
        RedisLockRegistry(redisConnectionFactory, "lock-registry")
}
