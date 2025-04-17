package io.clroot.ball.shared.lock

import io.clroot.ball.shared.lock.exception.LockKeyResolutionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SpelDistributedLockAnnotationProcessorTest : FunSpec({
    val processor = SpelDistributedLockAnnotationProcessor()

    test("should resolve key with literal expression") {
        val annotation = DistributedLock(key = "'literal-key'")
        val args = arrayOf<Any?>()
        val parameterNames = arrayOf<String>()

        val result = processor.resolveKey(annotation, args, parameterNames)

        result shouldBe "literal-key"
    }

    test("should resolve key with parameter reference") {
        val annotation = DistributedLock(key = "#userId")
        val args = arrayOf<Any?>("user123")
        val parameterNames = arrayOf("userId")

        val result = processor.resolveKey(annotation, args, parameterNames)

        result shouldBe "user123"
    }

    test("should resolve key with complex expression") {
        val annotation = DistributedLock(key = "'user-' + #userId + '-order-' + #orderId")
        val args = arrayOf<Any?>("user123", 456)
        val parameterNames = arrayOf("userId", "orderId")

        val result = processor.resolveKey(annotation, args, parameterNames)

        result shouldBe "user-user123-order-456"
    }

    test("should handle null parameter values") {
        val annotation = DistributedLock(key = "'prefix-' + (#userId != null ? #userId : 'anonymous')")
        val args = arrayOf<Any?>(null)
        val parameterNames = arrayOf("userId")

        val result = processor.resolveKey(annotation, args, parameterNames)

        result shouldBe "prefix-anonymous"
    }

    test("should throw LockKeyResolutionException when expression evaluates to null") {
        val annotation = DistributedLock(key = "#nonExistentVariable")
        val args = arrayOf<Any?>()
        val parameterNames = arrayOf<String>()

        val exception = shouldThrow<LockKeyResolutionException> {
            processor.resolveKey(annotation, args, parameterNames)
        }

        exception.message shouldBe "Lock key expression evaluated to null"
    }

    test("should handle multiple parameters") {
        val annotation = DistributedLock(key = "'order-' + #orderId + '-product-' + #productId + '-user-' + #userId")
        val args = arrayOf<Any?>(123, "prod456", "user789")
        val parameterNames = arrayOf("orderId", "productId", "userId")

        val result = processor.resolveKey(annotation, args, parameterNames)

        result shouldBe "order-123-product-prod456-user-user789"
    }

    test("should handle method parameters with default values") {
        // Simulating a method with some parameters having default values (not all args provided)
        val annotation = DistributedLock(key = "'user-' + #userId + (#action != null ? '-' + #action : '')")
        val args = arrayOf<Any?>("user123", null)
        val parameterNames = arrayOf("userId", "action")

        val result = processor.resolveKey(annotation, args, parameterNames)

        result shouldBe "user-user123"
    }
})