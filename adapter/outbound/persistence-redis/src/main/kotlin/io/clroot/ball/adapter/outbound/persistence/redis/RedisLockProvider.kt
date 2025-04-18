package io.clroot.ball.adapter.outbound.persistence.redis

import io.clroot.ball.shared.lock.LockProvider
import io.clroot.ball.shared.lock.exception.LockAcquisitionException
import org.springframework.context.annotation.Primary
import org.springframework.integration.redis.util.RedisLockRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Redis 기반 분산 락 제공자 구현
 */
@Component
@Primary
class RedisLockProvider(
    private val lockRegistry: RedisLockRegistry
) : LockProvider {

    /**
     * Redis 기반 락을 획득하고 함수를 실행한 후 락을 해제합니다.
     */
    override fun <T> withLock(key: String, waitTime: Long, leaseTime: Long, block: () -> T): T {
        val lock = lockRegistry.obtain(key)
        try {
            val acquired = lock.tryLock(waitTime, TimeUnit.MILLISECONDS)
            if (!acquired) {
                throw LockAcquisitionException("Failed to acquire lock for key: $key")
            }
            return block()
        } finally {
            try {
                lock.unlock()
            } catch (e: Exception) {
                // 락이 이미 해제되었거나 다른 스레드에 의해 소유된 경우 무시
            }
        }
    }
}