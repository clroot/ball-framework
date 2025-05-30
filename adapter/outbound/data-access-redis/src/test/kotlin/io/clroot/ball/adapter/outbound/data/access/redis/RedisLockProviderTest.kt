package io.clroot.ball.adapter.outbound.data.access.redis

import com.redis.testcontainers.RedisContainer
import io.clroot.ball.shared.lock.exception.LockAcquisitionException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestRedisConfiguration::class])
@Testcontainers
class RedisLockProviderTest {

    companion object {
        @Container
        private val redisContainer = RedisContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun registerRedisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.redis.host") { redisContainer.host }
            registry.add("spring.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }

    @Autowired
    private lateinit var redisLockProvider: RedisLockProvider

    @Test
    fun `should acquire and release lock successfully`() {
        // given
        val key = "test-lock-key"
        var executed = false

        // when
        val result = redisLockProvider.withLock(key, 1000, 10000) {
            executed = true
            "success"
        }

        // then
        assertTrue(executed)
        assertEquals("success", result)
    }

    @Test
    fun `should throw exception when lock acquisition fails`() {
        // given
        val key = "test-lock-key-2"
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(1)
        var lockHeld = false

        // First thread acquires and holds the lock
        executor.submit {
            redisLockProvider.withLock(key, 1000, 10000) {
                lockHeld = true
                latch.countDown()
                // Hold the lock for a while
                Thread.sleep(2000)
                lockHeld = false
                "first thread result"
            }
        }

        // Wait until the first thread has acquired the lock
        latch.await(2, TimeUnit.SECONDS)
        assertTrue(lockHeld, "First thread should have acquired the lock")

        // Second thread tries to acquire the same lock with a short wait time
        assertThrows<LockAcquisitionException> {
            redisLockProvider.withLock(key, 100, 10000) {
                "second thread result"
            }
        }

        // Clean up
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
        assertFalse(lockHeld, "Lock should be released after test")
    }
}
