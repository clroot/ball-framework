package io.clroot.ball.shared.lock.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LockExceptionsTest : FunSpec({

    test("LockAcquisitionException should be created with message") {
        val message = "Failed to acquire lock"
        val exception = LockAcquisitionException(message)

        exception.message shouldBe message
        exception.shouldBeInstanceOf<LockAcquisitionException>()
    }

    test("LockKeyResolutionException should be created with message") {
        val message = "Failed to resolve lock key"
        val exception = LockKeyResolutionException(message)

        exception.message shouldBe message
        exception.shouldBeInstanceOf<LockKeyResolutionException>()
    }

    test("LockAcquisitionException should extend ApplicationException") {
        val exception = LockAcquisitionException("test")

        exception.shouldBeInstanceOf<io.clroot.ball.shared.core.exception.ApplicationException>()
    }

    test("LockKeyResolutionException should extend ApplicationException") {
        val exception = LockKeyResolutionException("test")

        exception.shouldBeInstanceOf<io.clroot.ball.shared.core.exception.ApplicationException>()
    }
})