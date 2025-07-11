package io.clroot.ball.shared.lock

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import java.util.concurrent.TimeUnit

class DistributedLockAdvisorTest : FunSpec({
    val lockProvider = mockk<LockProvider>()
    val annotationProcessor = mockk<DistributedLockAnnotationProcessor>()
    val advisor = DistributedLockAdvisor(lockProvider, annotationProcessor)

    test("should acquire lock, execute method, and release lock") {
        // Mock method with @DistributedLock annotation
        val method = TestService::class.java.getMethod("testMethod", String::class.java, Int::class.java)
        val annotation = method.getAnnotation(DistributedLock::class.java)

        requireNotNull(annotation) { "Failed to resolve DistributedLock annotation" }

        // Mock method signature
        val signature = mockk<MethodSignature>()
        every { signature.method } returns method
        every { signature.parameterNames } returns arrayOf("userId", "orderId")

        // Mock join point
        val joinPoint = mockk<ProceedingJoinPoint>()
        every { joinPoint.signature } returns signature
        every { joinPoint.args } returns arrayOf<Any?>("user123", 456)
        every { joinPoint.proceed() } returns "result"

        // Mock annotation processor
        val resolvedKey = "user-user123-order-456"
        every {
            annotationProcessor.resolveKey(
                annotation,
                method,
                arrayOf<Any?>("user123", 456),
                arrayOf("userId", "orderId")
            )
        } returns resolvedKey

        // Mock lock provider
        val blockSlot = slot<() -> Any?>()
        every {
            lockProvider.withLock(
                resolvedKey,
                TimeUnit.SECONDS.toMillis(10), // waitTime from annotation
                TimeUnit.SECONDS.toMillis(3),  // leaseTime from annotation
                capture(blockSlot)
            )
        } answers { blockSlot.captured.invoke() }

        // Execute the advice
        val result = advisor.around(joinPoint)

        // Verify results
        result shouldBe "result"

        // Verify interactions
        verify {
            annotationProcessor.resolveKey(
                annotation,
                method,
                arrayOf<Any?>("user123", 456),
                arrayOf("userId", "orderId")
            )
        }
        verify {
            lockProvider.withLock(
                resolvedKey,
                TimeUnit.SECONDS.toMillis(10),
                TimeUnit.SECONDS.toMillis(3),
                captureLambda()
            )
        }
        verify { joinPoint.proceed() }
    }

    test("should use custom time units from annotation") {
        // Mock method with @DistributedLock annotation with custom time units
        val method = TestService::class.java.getMethod("testMethodWithCustomTimeUnits", String::class.java)
        val annotation = method.getAnnotation(DistributedLock::class.java)

        // Mock method signature
        val signature = mockk<MethodSignature>()
        every { signature.method } returns method
        every { signature.parameterNames } returns arrayOf("userId")

        // Mock join point
        val joinPoint = mockk<ProceedingJoinPoint>()
        every { joinPoint.signature } returns signature
        every { joinPoint.args } returns arrayOf<Any?>("user123")
        every { joinPoint.proceed() } returns "result"

        // Mock annotation processor
        val resolvedKey = "user-user123"
        every {
            annotationProcessor.resolveKey(
                annotation,
                method,
                arrayOf<Any?>("user123"),
                arrayOf("userId")
            )
        } returns resolvedKey

        // Mock lock provider
        val blockSlot = slot<() -> Any?>()
        every {
            lockProvider.withLock(
                resolvedKey,
                TimeUnit.MINUTES.toMillis(2), // waitTime from annotation (2 minutes)
                TimeUnit.MINUTES.toMillis(5), // leaseTime from annotation (5 minutes)
                capture(blockSlot)
            )
        } answers { blockSlot.captured.invoke() }

        // Execute the advice
        val result = advisor.around(joinPoint)

        // Verify results
        result shouldBe "result"

        // Verify interactions with correct time unit conversion
        verify {
            lockProvider.withLock(
                resolvedKey,
                TimeUnit.MINUTES.toMillis(2),
                TimeUnit.MINUTES.toMillis(5),
                captureLambda()
            )
        }
    }
}) {
    // Test classes for method annotations
    class TestService {
        @DistributedLock(key = "user-{userId}-order-{orderId}", waitTime = 10, leaseTime = 3)
        fun testMethod(@LockKey("userId") userId: String, @LockKey("orderId") orderId: Int): String {
            return "result"
        }

        @DistributedLock(
            key = "user-{userId}",
            timeUnit = TimeUnit.MINUTES,
            waitTime = 2,
            leaseTime = 5
        )
        fun testMethodWithCustomTimeUnits(@LockKey("userId") userId: String): String {
            return "result"
        }
    }
}
