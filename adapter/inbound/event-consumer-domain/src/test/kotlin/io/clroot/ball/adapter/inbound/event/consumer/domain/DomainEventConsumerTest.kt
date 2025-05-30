package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethod
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistry
import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

class DomainEventConsumerTest : BehaviorSpec({

    // 각 테스트 전후 MockK 상태 정리
    afterTest { _ ->
        clearAllMocks()
    }

    given("SpringDomainEventConsumer with port-based handlers") {
        val mockHandlerRegistry = mockk<EventHandlerRegistry>()
        val properties = DomainEventConsumerProperties(
            async = false,  // 테스트에서는 동기 처리
            enableDebugLogging = true,
            enableRetry = true
        )
        val consumer = SpringDomainEventConsumer(properties, mockHandlerRegistry)

        `when`("valid domain event is received") {
            val testEvent = TestDomainEvent(
                id = "valid-${UUID.randomUUID()}", 
                type = "ValidTestEvent", 
                occurredAt = Instant.now()
            )
            
            // EventConsumerPort 기반 모킹
            val mockPortHandler = mockk<TestEventConsumerPort>()
            every { mockPortHandler.eventType } returns TestDomainEvent::class
            every { mockPortHandler.handlerName } returns "TestEventConsumerPort"
            every { mockPortHandler.order } returns 0
            every { mockPortHandler.async } returns false
            coEvery { mockPortHandler.consume(testEvent) } just Runs

            val handlerMethod = EventHandlerMethod(
                bean = mockPortHandler,
                method = TestEventConsumerPort::class.java.getMethod("consume", TestDomainEvent::class.java),
                eventType = TestDomainEvent::class.java,
                methodName = "TestEventConsumerPort.consume",
                async = false,
                order = 0
            )

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(handlerMethod)

            then("event should be processed successfully") {
                consumer.handleDomainEvent(testEvent)

                coVerify(exactly = 1) { mockPortHandler.consume(testEvent) }
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
            val testEvent = TestDomainEvent(
                id = "exception-retry-${UUID.randomUUID()}", 
                type = "ExceptionRetryTestEvent", 
                occurredAt = Instant.now()
            )
            
            val mockHandler1 = mockk<EventHandlerMethod>()
            val mockHandler2 = mockk<EventHandlerMethod>()
            val exception = RuntimeException("Handler failed in retry test")

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(mockHandler1, mockHandler2)
            every { mockHandler1.methodName } returns "ExceptionHandler1.handle"
            every { mockHandler2.methodName } returns "ExceptionHandler2.handle"
            every { mockHandler1.order } returns 0
            every { mockHandler2.order } returns 1
            coEvery { mockHandler1.invoke(testEvent) } throws exception
            coEvery { mockHandler2.invoke(testEvent) } just Runs

            then("other handlers should still be executed") {
                runCatching { consumer.handleDomainEvent(testEvent) }

                coVerify(exactly = 1) { mockHandler1.invoke(testEvent) }
                coVerify(exactly = 1) { mockHandler2.invoke(testEvent) }
                verify(exactly = 1) { mockHandlerRegistry.getHandlers(testEvent.javaClass) }
            }
        }

        `when`("multiple handlers with different orders") {
            val testEvent = TestDomainEvent(
                id = "ordered-${UUID.randomUUID()}", 
                type = "OrderedTestEvent", 
                occurredAt = Instant.now()
            )
            
            val handler1 = mockk<EventHandlerMethod>()
            val handler2 = mockk<EventHandlerMethod>()
            val handler3 = mockk<EventHandlerMethod>()

            every { handler1.order } returns 2
            every { handler2.order } returns 1  
            every { handler3.order } returns 0
            every { handler1.methodName } returns "Handler1.handle"
            every { handler2.methodName } returns "Handler2.handle"
            every { handler3.methodName } returns "Handler3.handle"
            coEvery { handler1.invoke(testEvent) } just Runs
            coEvery { handler2.invoke(testEvent) } just Runs
            coEvery { handler3.invoke(testEvent) } just Runs

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(handler1, handler2, handler3)

            then("handlers should be executed in order") {
                consumer.handleDomainEvent(testEvent)

                // 모든 핸들러가 호출되었는지 확인
                coVerify(exactly = 1) { handler1.invoke(testEvent) }
                coVerify(exactly = 1) { handler2.invoke(testEvent) }
                coVerify(exactly = 1) { handler3.invoke(testEvent) }
            }
        }
    }

    given("SpringDomainEventConsumer with async processing") {
        val mockHandlerRegistry = mockk<EventHandlerRegistry>()
        val properties = DomainEventConsumerProperties(
            async = true,
            enableDebugLogging = true
        )
        val consumer = SpringDomainEventConsumer(properties, mockHandlerRegistry)

        `when`("async event is processed") {
            val testEvent = TestDomainEvent(
                id = "async-${UUID.randomUUID()}", 
                type = "AsyncTestEvent", 
                occurredAt = Instant.now()
            )
            val mockHandler = mockk<EventHandlerMethod>()

            every { mockHandlerRegistry.getHandlers(testEvent.javaClass) } returns listOf(mockHandler)
            every { mockHandler.methodName } returns "AsyncTestHandler.handle"
            every { mockHandler.order } returns 0
            coEvery { mockHandler.invoke(testEvent) } coAnswers {
                delay(100)  // 비동기 처리 시뮬레이션
            }

            then("event should be processed asynchronously") {
                consumer.handleDomainEvent(testEvent)

                coVerify(timeout = 2000) { mockHandler.invoke(testEvent) }
            }
        }

        afterSpec {
            consumer.shutdown()
        }
    }

    given("SpringDomainEventConsumer with disabled state") {
        val mockHandlerRegistry = mockk<EventHandlerRegistry>()
        val properties = DomainEventConsumerProperties(
            enabled = false,
            enableDebugLogging = true
        )
        val consumer = SpringDomainEventConsumer(properties, mockHandlerRegistry)

        `when`("event is received while disabled") {
            val testEvent = TestDomainEvent(
                id = "disabled-${UUID.randomUUID()}", 
                type = "DisabledTestEvent", 
                occurredAt = Instant.now()
            )

            then("event should be ignored") {
                consumer.handleDomainEvent(testEvent)

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

/**
 * 테스트용 EventConsumerPort 구현체
 */
interface TestEventConsumerPort : EventConsumerPort<TestDomainEvent> {
    override val eventType: KClass<TestDomainEvent> get() = TestDomainEvent::class
}
