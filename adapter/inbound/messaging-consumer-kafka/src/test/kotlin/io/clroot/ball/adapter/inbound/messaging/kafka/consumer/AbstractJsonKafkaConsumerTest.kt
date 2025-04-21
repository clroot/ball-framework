package io.clroot.ball.adapter.inbound.messaging.kafka.consumer

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.clroot.ball.adapter.inbound.messaging.core.MessageMetadata
import io.clroot.ball.adapter.inbound.messaging.kafka.config.KafkaConsumerProperties
import io.clroot.ball.shared.core.exception.DomainError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import java.time.Instant

class AbstractJsonKafkaConsumerTest : FunSpec({

    lateinit var objectMapper: ObjectMapper
    lateinit var properties: KafkaConsumerProperties
    lateinit var consumer: TestJsonKafkaConsumer
    lateinit var metadata: MessageMetadata

    beforeTest {
        objectMapper = jacksonObjectMapper()
        properties = KafkaConsumerProperties(consumerGroupIdPrefix = "test-consumer")
        consumer = TestJsonKafkaConsumer(objectMapper, properties)
        metadata = MessageMetadata("test-id", Instant.now())
    }

    test("getConsumerGroupId should return prefix + topic name") {
        // When
        val groupId = consumer.getConsumerGroupId()

        // Then
        groupId shouldBe "test-consumer-test-topic"
    }

    test("testToDomainObject should deserialize valid JSON") {
        // Given
        val jsonPayload = """{"name":"Test User","age":30}"""

        // When
        val result = runBlocking {
            consumer.testToDomainObject(jsonPayload, metadata)
        }

        // Then
        val domainObject = result.shouldBeRight()
        domainObject.name shouldBe "Test User"
        domainObject.age shouldBe 30
    }

    test("testToDomainObject should handle invalid JSON") {
        // Given
        val invalidJson = """{"name":"Test User", "age":invalid}"""

        // When
        val result = runBlocking {
            consumer.testToDomainObject(invalidJson, metadata)
        }

        // Then
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.MessagingError>()
        error.message shouldContain "Failed to deserialize JSON payload"
        error.messageId shouldBe "test-id"
        error.topic shouldBe "test-topic"
    }

    test("testToDomainObject should handle mapping errors") {
        // Given
        val jsonPayload = """{"name":"error-mapping","age":30}"""

        // When
        val result = runBlocking {
            consumer.testToDomainObject(jsonPayload, metadata)
        }

        // Then
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ValidationError>()
        error.exception.message shouldBe "Mapping error"
    }
})

// Test data class for JSON deserialization
private data class TestPayload(val name: String, val age: Int)

// Test implementation of AbstractJsonKafkaConsumer
private class TestJsonKafkaConsumer(
    objectMapper: ObjectMapper,
    properties: KafkaConsumerProperties
) : AbstractJsonKafkaConsumer<TestPayload, TestPayload>(objectMapper, properties) {

    override val payloadClass: Class<TestPayload> = TestPayload::class.java

    override fun getTopicName(): String = "test-topic"

    override suspend fun mapToDomainObject(
        deserializedPayload: TestPayload,
        metadata: MessageMetadata
    ): Either<DomainError, TestPayload> {
        return when (deserializedPayload.name) {
            "error-mapping" -> DomainError.ValidationError("Mapping error").left()
            else -> deserializedPayload.right()
        }
    }

    override suspend fun processDomainObject(
        domainObject: TestPayload,
        metadata: MessageMetadata
    ): Either<DomainError, Unit> {
        return Unit.right()
    }

    // Expose protected method for testing
    suspend fun testToDomainObject(payload: String, metadata: MessageMetadata): Either<DomainError, TestPayload> {
        return toDomainObject(payload, metadata)
    }
}
