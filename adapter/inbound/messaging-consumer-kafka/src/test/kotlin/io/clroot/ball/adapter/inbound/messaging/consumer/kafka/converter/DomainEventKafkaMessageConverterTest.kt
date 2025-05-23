package io.clroot.ball.adapter.inbound.messaging.consumer.kafka.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.time.Instant

class DomainEventKafkaMessageConverterTest : BehaviorSpec({

    val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }
    val converter = DomainEventKafkaMessageConverter(objectMapper)

    given("DomainEventKafkaMessageConverter") {
        `when`("올바른 형식의 JSON 메시지를 변환하는 경우") {
            val validMessage = """
                {
                    "eventType": "io.clroot.ball.adapter.inbound.messaging.consumer.kafka.converter.TestKafkaEvent",
                    "eventId": "test-id-123",
                    "occurredAt": "2023-01-01T00:00:00Z",
                    "eventData": {
                        "id": "test-id-123",
                        "data": "test-data",
                        "value": 42,
                        "occurredAt": "2023-01-01T00:00:00Z",
                        "type": "TestKafkaEvent"
                    }
                }
            """.trimIndent()

            then("도메인 이벤트로 변환되어야 한다") {
                val result = converter.convertToDomainEvent(validMessage)

                result shouldNotBe null
                result!!.id shouldBe "test-id-123"
                result.type shouldBe "TestKafkaEvent"
                (result as TestKafkaEvent).data shouldBe "test-data"
                (result as TestKafkaEvent).value shouldBe 42
            }
        }

        `when`("eventType이 없는 JSON 메시지인 경우") {
            val invalidMessage = """
                {
                    "eventId": "test-id-123",
                    "eventData": {
                        "id": "test-id-123",
                        "data": "test-data"
                    }
                }
            """.trimIndent()

            then("null을 반환해야 한다") {
                val result = converter.convertToDomainEvent(invalidMessage)
                result shouldBe null
            }
        }

        `when`("eventData가 없는 JSON 메시지인 경우") {
            val invalidMessage = """
                {
                    "eventType": "io.clroot.ball.adapter.inbound.messaging.consumer.kafka.converter.TestKafkaEvent",
                    "eventId": "test-id-123"
                }
            """.trimIndent()

            then("null을 반환해야 한다") {
                val result = converter.convertToDomainEvent(invalidMessage)
                result shouldBe null
            }
        }

        `when`("존재하지 않는 이벤트 타입인 경우") {
            val invalidMessage = """
                {
                    "eventType": "com.nonexistent.EventType",
                    "eventId": "test-id-123",
                    "eventData": {
                        "id": "test-id-123",
                        "data": "test-data"
                    }
                }
            """.trimIndent()

            then("null을 반환해야 한다") {
                val result = converter.convertToDomainEvent(invalidMessage)
                result shouldBe null
            }
        }

        `when`("잘못된 JSON 형식인 경우") {
            val malformedMessage = """
                {
                    "eventType": "TestEvent"
                    "eventData": {
                        "id": "test-id-123"
                    }
                // Missing closing brace
            """.trimIndent()

            then("null을 반환해야 한다") {
                val result = converter.convertToDomainEvent(malformedMessage)
                result shouldBe null
            }
        }

        `when`("메시지 유효성을 검증하는 경우") {
            val validMessage = """
                {
                    "eventType": "TestEvent",
                    "eventData": {
                        "id": "test-id-123",
                        "data": "test-data"
                    }
                }
            """.trimIndent()

            val invalidMessage = """
                {
                    "eventType": "TestEvent"
                }
            """.trimIndent()

            then("올바른 메시지는 valid, 잘못된 메시지는 invalid로 판단해야 한다") {
                converter.isValidMessage(validMessage) shouldBe true
                converter.isValidMessage(invalidMessage) shouldBe false
            }
        }

        `when`("이벤트 타입을 추출하는 경우") {
            val message = """
                {
                    "eventType": "io.clroot.ball.TestEvent",
                    "eventData": {
                        "id": "test-id-123"
                    }
                }
            """.trimIndent()

            then("이벤트 타입이 추출되어야 한다") {
                val eventType = converter.extractEventType(message)
                eventType shouldBe "io.clroot.ball.TestEvent"
            }
        }

        `when`("이벤트 ID를 추출하는 경우") {
            val message = """
                {
                    "eventType": "TestEvent",
                    "eventData": {
                        "id": "extracted-id-456",
                        "data": "test-data"
                    }
                }
            """.trimIndent()

            then("이벤트 ID가 추출되어야 한다") {
                val eventId = converter.extractEventId(message)
                eventId shouldBe "extracted-id-456"
            }
        }

        `when`("DomainEvent 인터페이스를 구현하지 않은 클래스인 경우") {
            val invalidMessage = """
                {
                    "eventType": "java.lang.String",
                    "eventData": {
                        "value": "not-a-domain-event"
                    }
                }
            """.trimIndent()

            then("null을 반환해야 한다") {
                val result = converter.convertToDomainEvent(invalidMessage)
                result shouldBe null
            }
        }
    }
})

/**
 * 테스트용 도메인 이벤트
 */
data class TestKafkaEvent(
    val data: String = "default-data",
    val value: Int = 0
) : DomainEvent {
    override val id: String = "kafka-test-id-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "TestKafkaEvent"
}
