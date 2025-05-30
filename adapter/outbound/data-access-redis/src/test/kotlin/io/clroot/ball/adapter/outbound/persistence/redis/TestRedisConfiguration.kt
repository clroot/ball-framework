package io.clroot.ball.adapter.outbound.persistence.redis

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.integration.redis.util.RedisLockRegistry
import java.time.Duration

@TestConfiguration
@EnableAutoConfiguration
class TestRedisConfiguration {

    @Bean
    fun redisConnectionFactory(environment: Environment): RedisConnectionFactory {
        val host = environment.getProperty("spring.redis.host", "localhost")
        val port = environment.getProperty("spring.redis.port", Int::class.java, 6379)

        val configuration = RedisStandaloneConfiguration(host, port)
        return LettuceConnectionFactory(configuration)
    }

    @Bean
    fun redisLockRegistry(redisConnectionFactory: RedisConnectionFactory): RedisLockRegistry {
        return RedisLockRegistry(redisConnectionFactory, "test-locks", Duration.ofSeconds(30).toMillis())
    }

    @Bean
    fun redisLockProvider(redisLockRegistry: RedisLockRegistry): RedisLockProvider {
        return RedisLockProvider(redisLockRegistry)
    }
}
