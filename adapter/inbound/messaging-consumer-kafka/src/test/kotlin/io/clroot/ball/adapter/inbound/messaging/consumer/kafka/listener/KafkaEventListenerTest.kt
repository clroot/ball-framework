package io.clroot.ball.adapter.inbound.messaging.consumer.kafka.listener

import io.clroot.ball.adapter.inbound.messaging.consumer.core.executor.DomainEventHandlerExecutor
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.KafkaEventConsumerProperties
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.converter.DomainEventKafkaMessageConverter
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.support.Acknowledgment
import java.time.Instant

class KafkaEventListenerTest : BehaviorSpec({

    given("KafkaEventListener") {
        val mockHandlerExecutor = mockk<DomainEventHandlerExecutor>()
        val mockMessageConverter = mockk<DomainEventKafkaMessageConverter>()
        val mockAcknowledgment = mockk<Acknowledgment>()
        
        val properties = KafkaEventConsumerProperties(
            async = false, // 테스트에서는 동기 처리
            enableDlq = false, // 테스트 단순화
            enableRetry = false
        )
        
        val listener = KafkaEventListener(mockHandlerExecutor, properties, mockMessageConverter)

        beforeEach {
            clearAllMocks()
        }

        `when`("올바른 Kafka 메시지를 수신하는 경우") {
            val validMessage = """{"eventType":"TestEvent","eventData":{"id":"test-123"}}"""
            val testEvent = TestKafkaListenerEvent("test-data")

            every { mockMessageConverter.isValidMessage(validMessage) } returns true
            every { mockMessageConverter.convertToDomainEvent(validMessage) } returns testEvent
            coEvery { mockHandlerExecutor.execute(testEvent) } just Runs
            every { mockAcknowledgment.acknowledge() } just Runs

            then("메시지가 변환되고 핸들러가 실행되며 오프셋이 커밋되어야 한다") {
                listener.handleKafkaMessage(
                    message = validMessage,
                    topic = "test-topic",
                    partition = 0,
                    offset = 123L,
                    acknowledgment = mockAcknowledgment
                )

                verify { mockMessageConverter.isValidMessage(validMessage) }
                verify { mockMessageConverter.convertToDomainEvent(validMessage) }
                coVerify { mockHandlerExecutor.execute(testEvent) }
                verify { mockAcknowledgment.acknowledge() }
            }
        }

        `when`("잘못된 형식의 메시지를 수신하는 경우") {
            val invalidMessage = """{"invalid":"message"}"""

            every { mockMessageConverter.isValidMessage(invalidMessage) } returns false
            every { mockAcknowledgment.acknowledge() } just Runs

            then("메시지가 스킵되고 오프셋만 커밋되어야 한다") {
                listener.handleKafkaMessage(
                    message = invalidMessage,
                    topic = "test-topic",
                    partition = 0,
                    offset = 124L,
                    acknowledgment = mockAcknowledgment
                )

                verify { mockMessageConverter.isValidMessage(invalidMessage) }
                verify(exactly = 0) { mockMessageConverter.convertToDomainEvent(any()) }
                coVerify(exactly = 0) { mockHandlerExecutor.execute(any()) }
                verify { mockAcknowledgment.acknowledge() }
            }
        }

        `when`("메시지 변환에 실패하는 경우") {
            val validFormatMessage = """{"eventType":"TestEvent","eventData":{"id":"test-123"}}"""

            every { mockMessageConverter.isValidMessage(validFormatMessage) } returns true
            every { mockMessageConverter.convertToDomainEvent(validFormatMessage) } returns null
            every { mockAcknowledgment.acknowledge() } just Runs

            then("변환 실패 메시지로 처리되고 오프셋이 커밋되어야 한다") {
                listener.handleKafkaMessage(
                    message = validFormatMessage,
                    topic = "test-topic",
                    partition = 0,
                    offset = 125L,
                    acknowledgment = mockAcknowledgment
                )

                verify { mockMessageConverter.isValidMessage(validFormatMessage) }
                verify { mockMessageConverter.convertToDomainEvent(validFormatMessage) }
                coVerify(exactly = 0) { mockHandlerExecutor.execute(any()) }
                verify { mockAcknowledgment.acknowledge() }
            }
        }

        `when`("핸들러 실행 중 예외가 발생하는 경우") {
            val validMessage = """{"eventType":"TestEvent","eventData":{"id":"test-123"}}"""
            val testEvent = TestKafkaListenerEvent("test-data")
            val exception = RuntimeException("Handler execution failed")

            every { mockMessageConverter.isValidMessage(validMessage) } returns true
            every { mockMessageConverter.convertToDomainEvent(validMessage) } returns testEvent
            coEvery { mockHandlerExecutor.execute(testEvent) } throws exception
            every { mockAcknowledgment.acknowledge() } just Runs

            then("예외가 처리되고 오프셋이 커밋되어야 한다") {
                listener.handleKafkaMessage(
                    message = validMessage,
                    topic = "test-topic",
                    partition = 0,
                    offset = 126L,
                    acknowledgment = mockAcknowledgment
                )

                verify { mockMessageConverter.isValidMessage(validMessage) }
                verify { mockMessageConverter.convertToDomainEvent(validMessage) }
                coVerify { mockHandlerExecutor.execute(testEvent) }
                verify { mockAcknowledgment.acknowledge() } // 실패한 메시지도 커밋
            }
        }

        `when`("비동기 모드에서 메시지를 처리하는 경우") {
            val asyncProperties = KafkaEventConsumerProperties(
                async = true,
                enableDlq = false,
                enableRetry = false
            )
            val asyncListener = KafkaEventListener(mockHandlerExecutor, asyncProperties, mockMessageConverter)
            
            val validMessage = """{"eventType":"TestEvent","eventData":{"id":"test-123"}}"""
            val testEvent = TestKafkaListenerEvent("async-test-data")

            every { mockMessageConverter.isValidMessage(validMessage) } returns true
            every { mockMessageConverter.convertToDomainEvent(validMessage) } returns testEvent
            coEvery { mockHandlerExecutor.execute(testEvent) } just Runs
            every { mockAcknowledgment.acknowledge() } just Runs

            then("즉시 오프셋이 커밋되어야 한다") {
                asyncListener.handleKafkaMessage(
                    message = validMessage,
                    topic = "async-topic",
                    partition = 1,
                    offset = 200L,
                    acknowledgment = mockAcknowledgment
                )

                verify { mockMessageConverter.isValidMessage(validMessage) }
                verify { mockMessageConverter.convertToDomainEvent(validMessage) }
                verify { mockAcknowledgment.acknowledge() } // 비동기에서는 즉시 커밋
                
                // 핸들러 실행은 비동기로 진행되므로 바로 검증하지 않음
            }
        }

        `when`("acknowledgment가 null인 경우") {
            val validMessage = """{"eventType":"TestEvent","eventData":{"id":"test-123"}}"""
            val testEvent = TestKafkaListenerEvent("no-ack-test")

            every { mockMessageConverter.isValidMessage(validMessage) } returns true
            every { mockMessageConverter.convertToDomainEvent(validMessage) } returns testEvent
            coEvery { mockHandlerExecutor.execute(testEvent) } just Runs

            then("오류 없이 처리되어야 한다") {
                listener.handleKafkaMessage(
                    message = validMessage,
                    topic = "test-topic",
                    partition = 0,
                    offset = 127L,
                    acknowledgment = null
                )

                verify { mockMessageConverter.isValidMessage(validMessage) }
                verify { mockMessageConverter.convertToDomainEvent(validMessage) }
                coVerify { mockHandlerExecutor.execute(testEvent) }
                // acknowledgment?.acknowledge() 호출되지만 null이므로 아무 일 없음
            }
        }
    }
})

/**
 * 테스트용 도메인 이벤트
 */
private data class TestKafkaListenerEvent(
    val data: String
) : DomainEvent {
    override val id: String = "kafka-listener-test-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "TestKafkaListenerEvent"
}
