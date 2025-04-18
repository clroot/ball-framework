package io.clroot.ball.adapter.inbound.messaging.core.retry

import arrow.core.Either
import io.clroot.ball.adapter.inbound.messaging.core.MessageConsumer
import io.clroot.ball.adapter.inbound.messaging.core.MessageDispatchException
import io.clroot.ball.adapter.inbound.messaging.core.MessageMetadata
import io.clroot.ball.shared.core.exception.DomainError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.runBlocking
import java.time.Instant

class MessageRetryHandlerTest : FunSpec({
    lateinit var consumer: MessageConsumer<String>
    lateinit var retryPolicy: RetryPolicy
    lateinit var handler: MessageRetryHandler<String>
    lateinit var metadata: MessageMetadata

    beforeTest {
        consumer = mockk<MessageConsumer<String>>()
        retryPolicy = mockk<RetryPolicy>()
        handler = MessageRetryHandler(consumer, retryPolicy)
        metadata = MessageMetadata("test-id", Instant.now())

        every { consumer.getTopicName() } returns "test-topic"
    }

    test("executeWithRetry should succeed on first try") {
        runBlocking {
            // Given
            val payload = "test-payload"
            val success = Either.Right(Unit)
            coEvery { consumer.consume(payload, metadata) } returns success
            coEvery { retryPolicy.maxRetries } returns 0

            // When
            val result = handler.executeWithRetry(payload, metadata)

            // Then
            result.shouldBeRight()
            coVerify(exactly = 1) { consumer.consume(payload, metadata) }
            verify(exactly = 1) { retryPolicy.maxRetries }
            verify(exactly = 0) { retryPolicy.isRetryable(any()) }
            verify(exactly = 0) { retryPolicy.calculateDelayDuration(any()) }
        }
    }

    test("executeWithRetry should retry with retryable exception") {
        runBlocking {
            // Given
            val payload = "test-payload"
            val exception = RuntimeException("Test exception")
            val success = Either.Right(Unit)

            coEvery { consumer.consume(payload, metadata) } throws exception andThen success
            every { retryPolicy.isRetryable(exception) } returns true
            every { retryPolicy.maxRetries } returns 3
            every { retryPolicy.calculateDelayDuration(1) } returns kotlin.time.Duration.ZERO

            // When
            val result = handler.executeWithRetry(payload, metadata)

            // Then
            result.shouldBeRight()
            coVerify(exactly = 2) { consumer.consume(payload, metadata) }
            verify(exactly = 1) { retryPolicy.isRetryable(exception) }
            verify(exactly = 1) { retryPolicy.calculateDelayDuration(1) }
        }
    }

    test("executeWithRetry should not retry with non-retryable exception") {
        runBlocking {
            // Given
            val payload = "test-payload"
            val exception = MessageDispatchException("Test exception", retryable = false)

            coEvery { consumer.consume(payload, metadata) } throws exception
            every { retryPolicy.isRetryable(exception) } returns false
            every { retryPolicy.maxRetries } returns 3

            // When
            val result = handler.executeWithRetry(payload, metadata)

            // Then
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<DomainError.MessagingError>()
            error.topic shouldBe "test-topic"
            error.messageId shouldBe "test-id"
            error.cause shouldBe exception

            coVerify(exactly = 1) { consumer.consume(payload, metadata) }
            verify(exactly = 1) { retryPolicy.isRetryable(exception) }
            verify(exactly = 0) { retryPolicy.calculateDelayDuration(any()) }
        }
    }

    test("executeWithRetry should stop after max retries") {
        runBlocking {
            // Given
            val payload = "test-payload"
            val exception = RuntimeException("Test exception")

            coEvery { consumer.consume(payload, metadata) } throws exception
            every { retryPolicy.isRetryable(exception) } returns true
            every { retryPolicy.maxRetries } returns 2
            every { retryPolicy.calculateDelayDuration(any()) } returns kotlin.time.Duration.ZERO

            // When
            val result = handler.executeWithRetry(payload, metadata)

            // Then
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<DomainError.MessagingError>()
            error.topic shouldBe "test-topic"
            error.messageId shouldBe "test-id"
            error.cause shouldBe exception

            coVerify(exactly = 3) { consumer.consume(payload, metadata) } // Initial + 2 retries
            verify(exactly = 3) { retryPolicy.isRetryable(exception) }
            verify(exactly = 2) { retryPolicy.calculateDelayDuration(any()) }
        }
    }
})
