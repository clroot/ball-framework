@file:Suppress("UNCHECKED_CAST")

package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethodFactory
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistryInterface
import io.clroot.ball.adapter.inbound.event.consumer.domain.DomainEventConsumerProperties
import io.clroot.ball.adapter.inbound.event.consumer.domain.SpringDomainEventConsumer
import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.event.Event

/**
 * 도메인 이벤트 컨슈머 테스트를 위한 헬퍼 클래스 - ThreadPool 기반
 *
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 * 복잡한 모킹과 설정을 캡슐화하여 테스트 작성을 단순화합니다.
 */
object DomainEventConsumerTestHelper {

    /**
     * 테스트용 SpringDomainEventConsumer 생성
     */
    fun createTestConsumer(
        handlers: List<EventConsumerPort<out Event>> = emptyList(),
        properties: DomainEventConsumerProperties = createTestProperties()
    ): SpringDomainEventConsumer {
        val registry = createTestRegistry(handlers)
        return SpringDomainEventConsumer(properties, registry)
    }

    /**
     * 테스트용 ThreadPool 기반 EventHandlerRegistry 생성
     */
    fun createTestRegistry(handlers: List<EventConsumerPort<out Event>>): EventHandlerRegistryInterface {
        val registry = TestEventHandlerRegistry()
        
        handlers.forEach { handler ->
            try {
                val eventType = handler.eventType.java as Class<out DomainEvent>
                
                // ThreadPool 기반 팩토리 메서드 사용
                val handlerMethod = EventHandlerMethodFactory.createForTest(handler, eventType)
                
                registry.registerHandler(eventType, handlerMethod)
                
                println("✅ Registered test handler: ${eventType.simpleName} -> ${handler.handlerName}")
                
            } catch (e: Exception) {
                println("❌ Failed to register test handler: ${handler.javaClass.simpleName} for ${handler.eventType.simpleName}")
                println("   Error: ${e.message}")
                throw IllegalArgumentException(
                    "Failed to register handler: ${handler.javaClass.simpleName} " +
                    "for event type: ${handler.eventType.simpleName}. " +
                    "Error: ${e.message}", e
                )
            }
        }
        
        return registry
    }

    /**
     * 테스트용 속성 생성 (ThreadPool 기반 최적화)
     */
    fun createTestProperties(
        enabled: Boolean = true,
        async: Boolean = true,  // ThreadPool에서는 기본적으로 비동기
        enableDebugLogging: Boolean = true,
        enableRetry: Boolean = true,
        continueOnError: Boolean = true,
        processInTransaction: Boolean = true,
        processAfterCommit: Boolean = false
    ): DomainEventConsumerProperties {
        return DomainEventConsumerProperties(
            enabled = enabled,
            async = async,
            enableDebugLogging = enableDebugLogging,
            enableRetry = enableRetry,
            continueOnError = continueOnError,
            processInTransaction = processInTransaction,    
            processAfterCommit = processAfterCommit
        )
    }

    /**
     * 테스트용 도메인 이벤트 생성 팩토리
     */
    fun createTestEvent(
        id: String = "test-${System.currentTimeMillis()}",
        type: String = "TestEvent",
        message: String = "Test message"
    ): TestDomainEvent {
        return TestDomainEvent(
            id = id,
            type = type,
            occurredAt = java.time.Instant.now(),
            message = message
        )
    }
    
    /**
     * 여러 테스트 이벤트를 일괄 생성
     */
    fun createTestEvents(count: Int, prefix: String = "test"): List<TestDomainEvent> {
        return (1..count).map { index ->
            createTestEvent(
                id = "$prefix-$index",
                type = "TestEvent$index",
                message = "Test message $index"
            )
        }
    }
    
    /**
     * 테스트용 핸들러 팩토리 메서드들
     */
    fun createSimpleHandler(): TestDomainEventHandler {
        return TestDomainEventHandler.create()
    }
    
    fun createDelayedHandler(delayMs: Long): TestDomainEventHandler {
        return TestDomainEventHandler.withDelay(delayMs)
    }
    
    fun createFailingHandler(exception: RuntimeException = RuntimeException("Test error")): TestDomainEventHandler {
        return TestDomainEventHandler.withException(exception)
    }
    
    /**
     * 테스트 시나리오 헬퍼 메서드들
     */
    fun executeAndWait(
        consumer: SpringDomainEventConsumer,
        events: List<TestDomainEvent>,
        waitTimeMs: Long = 1000
    ) {
        // 이벤트들 처리
        events.forEach { event ->
            consumer.handleDomainEvent(event)
        }
        
        // ThreadPool 처리 대기
        Thread.sleep(waitTimeMs)
    }
    
    fun executeAndWaitSingle(
        consumer: SpringDomainEventConsumer,
        event: TestDomainEvent,
        waitTimeMs: Long = 500
    ) {
        consumer.handleDomainEvent(event)
        Thread.sleep(waitTimeMs)
    }
}
