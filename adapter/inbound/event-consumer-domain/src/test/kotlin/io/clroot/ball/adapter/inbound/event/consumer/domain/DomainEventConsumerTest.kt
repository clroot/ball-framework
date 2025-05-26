package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethod
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistry
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.*

class DomainEventConsumerTest : BehaviorSpec({

    // 각 테스트 전후 MockK 상태 정리
    afterTest { _ ->
        clearAllMocks()
    }

    given("DomainEventConsumer") {
        val mockHandlerRegistry = mockk<EventHandlerRegistry>()
        val properties = DomainEventConsumerProperties(
            async = false,  // 테스트에서는 동기 처리
            enableDebugLogging = true,
            enableRetry = true
        )
        val consumer = DomainEventConsumer(properties, mockHandlerRegistry)

        `when`("valid domain event is received") {
            val testEvent = TestDomainEvent(
                id = "valid-${UUID.randomUUID()}", 
                type = "ValidTestEvent", 
                occurredAt = Instant.now()
            )
            val mockHandler = mockk<EventHandlerMethod>()

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(mockHandler)
            every { mockHandler.methodName } returns "ValidTestHandler.handleEvent"
            coEvery { mockHandler.invoke(testEvent) } just Runs

            then("event should be processed successfully") {
                consumer.handleDomainEvent(testEvent)

                coVerify(exactly = 1) { mockHandler.invoke(testEvent) }
            }
        }

        `when`("no handlers are found for event") {
            val testEvent = TestDomainEvent(
                id = "no-handler-${UUID.randomUUID()}", 
                type = "NoHandlerTestEvent", 
                occurredAt = Instant.now()
            )

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns emptyList()

            then("event should be processed without errors") {
                consumer.handleDomainEvent(testEvent)

                verify(exactly = 1) { mockHandlerRegistry.getHandlers(testEvent.javaClass) }
            }
        }

        `when`("handler throws exception with retry enabled") {
            // 고유한 테스트 데이터로 격리
            val testEvent = TestDomainEvent(
                id = "exception-retry-${UUID.randomUUID()}", 
                type = "ExceptionRetryTestEvent", 
                occurredAt = Instant.now()
            )
            val mockHandler1 = mockk<EventHandlerMethod>()
            val mockHandler2 = mockk<EventHandlerMethod>()
            val exception = RuntimeException("Handler failed in retry test")

            // Mock 설정을 명확하게 분리
            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(mockHandler1, mockHandler2)
            every { mockHandler1.methodName } returns "ExceptionHandler1.handleEvent"
            every { mockHandler2.methodName } returns "ExceptionHandler2.handleEvent"
            coEvery { mockHandler1.invoke(testEvent) } throws exception
            coEvery { mockHandler2.invoke(testEvent) } just Runs

            then("other handlers should still be executed") {
                // 예외가 발생해도 다른 핸들러들은 계속 실행되어야 함
                runCatching { consumer.handleDomainEvent(testEvent) }

                // 각 핸들러가 정확히 한 번씩 호출되었는지 검증
                coVerify(exactly = 1) { mockHandler1.invoke(testEvent) }
                coVerify(exactly = 1) { mockHandler2.invoke(testEvent) }
                verify(exactly = 1) { mockHandlerRegistry.getHandlers(testEvent.javaClass) }
            }
        }

        `when`("event with blank ID is processed") {
            val invalidEvent = TestDomainEvent(
                id = "", 
                type = "BlankIdTestEvent", 
                occurredAt = Instant.now()
            )

            then("should handle gracefully") {
                runCatching { consumer.handleDomainEvent(invalidEvent) }.isSuccess shouldBe true
            }
        }
    }

    given("DomainEventConsumer with retry disabled") {
        val mockHandlerRegistry = mockk<EventHandlerRegistry>()
        val properties = DomainEventConsumerProperties(
            async = false,
            enableRetry = false, // 재시도 비활성화
            enableDebugLogging = true
        )
        val consumer = DomainEventConsumer(properties, mockHandlerRegistry)

        `when`("handler throws exception") {
            val testEvent = TestDomainEvent(
                id = "exception-no-retry-${UUID.randomUUID()}", 
                type = "ExceptionNoRetryTestEvent", 
                occurredAt = Instant.now()
            )
            val mockHandler1 = mockk<EventHandlerMethod>()
            val mockHandler2 = mockk<EventHandlerMethod>()
            val exception = RuntimeException("Handler failed in no-retry test")

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(mockHandler1, mockHandler2)
            every { mockHandler1.methodName } returns "NoRetryHandler1.handleEvent"
            every { mockHandler2.methodName } returns "NoRetryHandler2.handleEvent"
            coEvery { mockHandler1.invoke(testEvent) } throws exception
            coEvery { mockHandler2.invoke(testEvent) } just Runs

            then("should handle exception according to retry policy") {
                runCatching { consumer.handleDomainEvent(testEvent) }

                coVerify(exactly = 1) { mockHandler1.invoke(testEvent) }
                verify(exactly = 1) { mockHandlerRegistry.getHandlers(testEvent.javaClass) }
            }
        }
    }

    given("DomainEventConsumer with async processing") {
        val mockHandlerRegistry = mockk<EventHandlerRegistry>()
        val properties = DomainEventConsumerProperties(
            async = true,
            enableDebugLogging = true
        )
        val consumer = DomainEventConsumer(properties, mockHandlerRegistry)

        `when`("async event is processed") {
            val testEvent = TestDomainEvent(
                id = "async-${UUID.randomUUID()}", 
                type = "AsyncTestEvent", 
                occurredAt = Instant.now()
            )
            val mockHandler = mockk<EventHandlerMethod>()

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(mockHandler)
            every { mockHandler.methodName } returns "AsyncTestHandler.handleEvent"
            coEvery { mockHandler.invoke(testEvent) } coAnswers {
                delay(100)  // 비동기 처리 시뮬레이션
            }

            then("event should be processed asynchronously") {
                consumer.handleDomainEvent(testEvent)

                // 테스트 스코프에서 비동기 작업 완료 대기
                coVerify(timeout = 2000) { mockHandler.invoke(testEvent) }
            }
        }

        afterSpec {
            // 비동기 consumer 정리
            consumer.shutdown()
        }
    }

    given("DomainEventConsumer with disabled state") {
        val mockHandlerRegistry = mockk<EventHandlerRegistry>()
        val properties = DomainEventConsumerProperties(
            enabled = false, // 소비자 비활성화
            enableDebugLogging = true
        )
        val consumer = DomainEventConsumer(properties, mockHandlerRegistry)

        `when`("event is received while disabled") {
            val testEvent = TestDomainEvent(
                id = "disabled-${UUID.randomUUID()}", 
                type = "DisabledTestEvent", 
                occurredAt = Instant.now()
            )

            then("event should be ignored") {
                consumer.handleDomainEvent(testEvent)

                // 비활성화 상태에서는 핸들러 레지스트리 조회조차 하지 않음
                verify(exactly = 0) { mockHandlerRegistry.getHandlers(any()) }
            }
        }
    }
})

/**
 * 테스트용 도메인 이벤트
 */
data class TestDomainEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant
) : DomainEvent
