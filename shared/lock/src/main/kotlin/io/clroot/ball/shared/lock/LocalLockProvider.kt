package io.clroot.ball.shared.lock

import io.clroot.ball.shared.lock.exception.LockAcquisitionException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * 로컬 메모리 기반 락 제공자 구현
 * 개발 및 테스트 환경에서 사용
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class LocalLockProvider : LockProvider {
    private val locks = mutableMapOf<String, Lock>()

    override fun <T> withLock(key: String, waitTime: Long, leaseTime: Long, block: () -> T): T {
        val lock = getLock(key)

        val acquired = lock.tryLock(waitTime, TimeUnit.MILLISECONDS)
        if (!acquired) {
            throw LockAcquisitionException("Failed to acquire lock for key: $key")
        }

        try {
            return block()
        } finally {
            try {
                lock.unlock()
            } catch (e: Exception) {
                // 락이 이미 해제되었거나 다른 스레드에 의해 소유된 경우 무시
            }
        }
    }

    private fun getLock(key: String): Lock {
        return locks.computeIfAbsent(key) { ReentrantLock() }
    }
}
