package io.clroot.ball.shared.lock

import io.clroot.ball.shared.lock.exception.LockAcquisitionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LocalLockProviderTest : FunSpec({
    val lockProvider = LocalLockProvider()
    val testKey = "test-lock-key"

    test("should execute block when lock is acquired") {
        val result = lockProvider.withLock(testKey) {
            "success"
        }

        result shouldBe "success"
    }

    test("should throw LockAcquisitionException when lock cannot be acquired") {
        // First thread acquires the lock and holds it
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(1)
        val lockHeld = AtomicInteger(0)

        // Thread 1: Acquire lock and hold it
        executor.submit {
            try {
                lockProvider.withLock(testKey, 10000, 10000) {
                    lockHeld.set(1)
                    latch.countDown() // Signal that lock is acquired
                    Thread.sleep(1000) // Hold the lock for 1 second
                    lockHeld.set(0)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Wait for the first thread to acquire the lock
        latch.await(5, TimeUnit.SECONDS)

        // Thread 2: Try to acquire the same lock with a short wait time
        val exception = shouldThrow<LockAcquisitionException> {
            lockProvider.withLock(testKey, 100, 1000) {
                // This should not execute
                "should not reach here"
            }
        }

        exception.message shouldBe "Failed to acquire lock for key: $testKey"
        lockHeld.get() shouldBe 1 // Verify the lock is still held by the first thread

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    test("should release lock after block execution") {
        // First execution should succeed
        lockProvider.withLock(testKey) {
            "first execution"
        }

        // Second execution should also succeed because the lock was released
        val result = lockProvider.withLock(testKey) {
            "second execution"
        }

        result shouldBe "second execution"
    }

    test("should release lock even if block throws exception") {
        // First execution throws exception
        shouldThrow<RuntimeException> {
            lockProvider.withLock(testKey) {
                throw RuntimeException("test exception")
            }
        }

        // Second execution should succeed because the lock was released
        val result = lockProvider.withLock(testKey) {
            "after exception"
        }

        result shouldBe "after exception"
    }
})