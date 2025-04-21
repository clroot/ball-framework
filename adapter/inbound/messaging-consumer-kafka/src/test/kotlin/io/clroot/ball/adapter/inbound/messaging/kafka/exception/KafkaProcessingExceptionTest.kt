package io.clroot.ball.adapter.inbound.messaging.kafka.exception

import io.clroot.ball.adapter.inbound.messaging.core.MessageDispatchException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.apache.kafka.clients.consumer.ConsumerRecord

class KafkaProcessingExceptionTest : FunSpec({

    test("KafkaProcessingException should extend MessageDispatchException") {
        // Given
        val exception = KafkaProcessingException(
            message = "Test message",
            cause = RuntimeException("Test cause"),
            messageId = "test-id",
            topic = "test-topic",
            retryable = true,
            record = ConsumerRecord("test-topic", 0, 0, "test-key", "test-value")
        )

        // Then
        exception.shouldBeInstanceOf<MessageDispatchException>()
        exception.message shouldBe "Test message [messageId=test-id, topic=test-topic, retryable=true]"
        exception.cause?.message shouldBe "Test cause"
        exception.messageId shouldBe "test-id"
        exception.topic shouldBe "test-topic"
        exception.retryable shouldBe true
        exception.record?.topic() shouldBe "test-topic"
        exception.record?.partition() shouldBe 0
        exception.record?.offset() shouldBe 0
        exception.record?.key() shouldBe "test-key"
        exception.record?.value() shouldBe "test-value"
    }

    test("nonRetryable factory method should create non-retryable exception") {
        // Given
        val record = ConsumerRecord("test-topic", 0, 0, "test-key", "test-value")

        // When
        val exception = KafkaProcessingException.nonRetryable(
            message = "Non-retryable error",
            cause = RuntimeException("Test cause"),
            messageId = "test-id",
            topic = "test-topic",
            record = record
        )

        // Then
        exception.retryable shouldBe false
        exception.message shouldBe "Non-retryable error [messageId=test-id, topic=test-topic, retryable=false]"
        exception.cause?.message shouldBe "Test cause"
        exception.messageId shouldBe "test-id"
        exception.topic shouldBe "test-topic"
        exception.record shouldBe record
    }
})