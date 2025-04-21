package io.clroot.ball.adapter.inbound.messaging.core

import arrow.core.Either
import io.clroot.ball.shared.core.exception.DomainError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import java.time.Instant

class AbstractMessageConsumerTest : FunSpec({

    lateinit var consumer: TestMessageConsumer
    lateinit var metadata: MessageMetadata

    beforeTest {
        consumer = TestMessageConsumer()
        metadata = MessageMetadata("test-id", Instant.now())
    }

    test("consume should successfully process message") {
        runBlocking {
            // Given
            val payload = "test-payload"

            // When
            val result = consumer.consume(payload, metadata)

            // Then
            result.shouldBeRight()
            consumer.processedDomainObject shouldBe "test-payload-processed"
        }
    }

    test("consume should handle conversion error") {
        runBlocking {
            // Given
            val payload = "error-conversion"

            // When
            val result = consumer.consume(payload, metadata)

            // Then
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<DomainError.ValidationError>()
            error.exception.message shouldBe "Conversion error"
        }
    }

    test("consume should handle processing error") {
        runBlocking {
            // Given
            val payload = "error-processing"

            // When
            val result = consumer.consume(payload, metadata)

            // Then
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<DomainError.BusinessRuleViolationError>()
            error.exception.message shouldBe "Processing error"
        }
    }

    test("consume should handle unexpected exception") {
        runBlocking {
            // Given
            val payload = "exception"

            // When
            val result = consumer.consume(payload, metadata)

            // Then
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<DomainError.MessagingError>()
            error.topic shouldBe "test-topic"
            error.messageId shouldBe "test-id"
            error.message shouldContain "Error processing message"
        }
    }
})

// Test implementation of AbstractMessageConsumer
private class TestMessageConsumer : AbstractMessageConsumer<String, String>() {
    var processedDomainObject: String? = null

    override fun getTopicName(): String = "test-topic"

    override suspend fun toDomainObject(payload: String, metadata: MessageMetadata): Either<DomainError, String> {
        return when (payload) {
            "error-conversion" -> Either.Left(DomainError.ValidationError(message = "Conversion error"))
            "exception" -> throw RuntimeException("Unexpected exception")
            else -> Either.Right("$payload-converted")
        }
    }

    override suspend fun processDomainObject(domainObject: String, metadata: MessageMetadata): Either<DomainError, Unit> {
        return when {
            domainObject.contains("error-processing") -> Either.Left(DomainError.BusinessRuleViolationError("Processing error"))
            else -> {
                processedDomainObject = "$domainObject-processed".replace("-converted-processed", "-processed")
                Either.Right(Unit)
            }
        }
    }
}
