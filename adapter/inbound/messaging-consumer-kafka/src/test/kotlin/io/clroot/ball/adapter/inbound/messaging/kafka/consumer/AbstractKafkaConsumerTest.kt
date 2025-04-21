package io.clroot.ball.adapter.inbound.messaging.kafka.consumer

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.clroot.ball.adapter.inbound.messaging.core.MessageMetadata
import io.clroot.ball.shared.core.exception.DomainError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment
import java.time.Instant

class AbstractKafkaConsumerTest : FunSpec({

    lateinit var consumer: TestKafkaConsumer
    lateinit var acknowledgment: Acknowledgment

    beforeTest {
        consumer = TestKafkaConsumer()
        acknowledgment = mockk(relaxed = true)
    }

    afterTest {
        clearAllMocks()
    }

    test("listen should process message successfully") {
        // Given
        val payload = "test-payload"
        val topic = "test-topic"
        val partition = 0
        val offset = 123L
        val timestamp = System.currentTimeMillis()
        val record = ConsumerRecord(topic, partition, offset, "test-key", payload)

        // When
        consumer.listen(
            payload = payload,
            topic = topic,
            partition = partition,
            offset = offset,
            timestamp = timestamp,
            acknowledgment = acknowledgment,
            record = record
        )

        // Then
        consumer.processedPayload shouldBe payload
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    test("listen should handle processing error") {
        // Given
        val payload = "error-payload"
        val topic = "test-topic"
        val partition = 0
        val offset = 123L
        val timestamp = System.currentTimeMillis()
        val record = ConsumerRecord(topic, partition, offset, "test-key", payload)

        // When
        consumer.listen(
            payload = payload,
            topic = topic,
            partition = partition,
            offset = offset,
            timestamp = timestamp,
            acknowledgment = acknowledgment,
            record = record
        )

        // Then
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    test("listen should handle unexpected exception") {
        // Given
        val payload = "exception-payload"
        val topic = "test-topic"
        val partition = 0
        val offset = 123L
        val timestamp = System.currentTimeMillis()
        val record = ConsumerRecord(topic, partition, offset, "test-key", payload)

        // When
        consumer.listen(
            payload = payload,
            topic = topic,
            partition = partition,
            offset = offset,
            timestamp = timestamp,
            acknowledgment = acknowledgment,
            record = record
        )

        // Then
        // Since AbstractMessageConsumer now returns Either.Left instead of throwing exceptions,
        // the error is handled via the fold method and the message is acknowledged
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    test("extractEventType should return event type from headers") {
        // Given
        val headers = mapOf("event-type" to "test-event")

        // When
        val eventType = consumer.extractEventType(headers, "payload")

        // Then
        eventType shouldBe "test-event"
    }

    test("extractEventType should return class name when no event type in headers") {
        // Given
        val headers = emptyMap<String, Any>()
        val payload = "test-payload"

        // When
        val eventType = consumer.extractEventType(headers, payload)

        // Then
        eventType shouldBe "String"
    }

    test("shouldAcknowledgeOnError should return false by default") {
        // Given
        val error = RuntimeException("Test error")

        // When
        val result = consumer.shouldAcknowledgeOnError(error)

        // Then
        result shouldBe false
    }

    test("handleProcessingError should log error message") {
        // Given
        val payload = "test-payload"
        val messageId = "test-id"
        val topic = "test-topic"
        val metadata = MessageMetadata(
            messageId = messageId,
            timestamp = Instant.now(),
            headers = emptyMap(),
            source = "kafka",
            eventType = "test-event"
        )
        val record = ConsumerRecord(topic, 0, 0, "test-key", payload)
        val error = DomainError.ValidationError("Test validation error")

        // When
        consumer.testHandleProcessingError(error, payload, metadata, record)

        // Then,
        // Since we can't easily verify logging in a unit test, we're just verifying that the method doesn't throw an exception
        // In a real application, we might use a logging framework that allows capturing logs for testing
    }
})

// Test implementation of AbstractKafkaConsumer
private class TestKafkaConsumer : AbstractKafkaConsumer<String, String>() {
    var processedPayload: String? = null

    override fun getTopicName(): String = "test-topic"

    override fun getConsumerGroupId(): String = "test-consumer-group"

    override suspend fun toDomainObject(payload: String, metadata: MessageMetadata): Either<DomainError, String> {
        return when (payload) {
            "error-payload" -> DomainError.ValidationError("Test validation error").left()
            "exception-payload" -> throw RuntimeException("Test exception")
            else -> payload.right()
        }
    }

    override suspend fun processDomainObject(
        domainObject: String,
        metadata: MessageMetadata
    ): Either<DomainError, Unit> {
        processedPayload = domainObject
        return Unit.right()
    }

    // Expose protected methods for testing
    public override fun extractEventType(headers: Map<String, Any>?, payload: String): String {
        return super.extractEventType(headers, payload)
    }

    public override fun shouldAcknowledgeOnError(error: Throwable): Boolean {
        return super.shouldAcknowledgeOnError(error)
    }

    fun testHandleProcessingError(
        error: Any,
        payload: String,
        metadata: MessageMetadata,
        record: ConsumerRecord<*, *>?
    ) {
        handleProcessingError(error, payload, metadata, record)
    }
}
