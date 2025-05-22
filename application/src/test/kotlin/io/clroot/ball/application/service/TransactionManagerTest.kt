package io.clroot.ball.application.service

import arrow.core.Either
import io.clroot.ball.application.port.outbound.TransactionManager
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TransactionManagerTest : StringSpec({
    "withTransaction with Either should return Right when block returns Right" {
        // Given
        val mockImpl = object : TransactionManager {
            override fun <T> withTransaction(block: () -> T): T = block()
            override fun <T> withNewTransaction(block: () -> T): T = block()
        }

        val expectedResult = "success"

        // When
        val result = mockImpl.withTransaction<String, String> { Either.Right(expectedResult) }

        // Then
        result shouldBe Either.Right(expectedResult)
    }

    "withTransaction with Either should return Left when block returns Left" {
        // Given
        val mockImpl = object : TransactionManager {
            override fun <T> withTransaction(block: () -> T): T = block()
            override fun <T> withNewTransaction(block: () -> T): T = block()
        }

        open class TestError

        val expectedError = object : TestError() {}

        // When
        val result = mockImpl.withTransaction<TestError, String> { Either.Left(expectedError) }

        // Then
        result shouldBe Either.Left(expectedError)
    }

    "withTransaction with Either should propagate exceptions that are not TransactionWrappedException" {
        // Given
        val mockImpl = object : TransactionManager {
            override fun <T> withTransaction(block: () -> T): T = block()
            override fun <T> withNewTransaction(block: () -> T): T {
                throw RuntimeException("Unexpected error")
            }
        }

        // When/Then
        val exception = runCatching {
            mockImpl.withTransaction<String, String> { Either.Right("success") }
        }.exceptionOrNull()

        exception shouldBe RuntimeException("Unexpected error")
    }
})
