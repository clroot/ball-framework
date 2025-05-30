package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethodFactory
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistryInterface
import io.clroot.ball.adapter.inbound.event.consumer.domain.DomainEventConsumerProperties
import io.clroot.ball.adapter.inbound.event.consumer.domain.SpringDomainEventConsumer
import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.event.Event

/**
 * 도메인 이벤트 컨슈머 테스트를 위한 헬퍼 클래스
 *
 * 복잡한 모킹과 설정을 캡슐화하여 테스트 작성을 단순화합니다.
 */
object DomainEventConsumerTestHelper {

    /**
     * 테스트용 SpringDomainEventConsumer 생성
     *
     * @param handlers 등록할 핸들러들
     * @param properties 컨슈머 설정 (기본값 제공)
     * @return 테스트용 컨슈머 인스턴스
     */
    fun createTestConsumer(
        handlers: List<EventConsumerPort<out Event>> = emptyList(),
        properties: DomainEventConsumerProperties = createTestProperties()
    ): SpringDomainEventConsumer {
        val registry = createTestRegistry(handlers)
        return SpringDomainEventConsumer(properties, registry)
    }

    /**
     * 테스트용 EventHandlerRegistry 생성
     */
    fun createTestRegistry(handlers: List<EventConsumerPort<out Event>>): EventHandlerRegistryInterface {
        val registry = TestEventHandlerRegistry()
        
        handlers.forEach { handler ->
            try {
                val eventType = handler.eventType.java as Class<out DomainEvent>
                
                // 테스트용 간소화된 팩토리 메서드 사용
                val handlerMethod = EventHandlerMethodFactory.createForTest(handler, eventType)
                
                registry.registerHandler(eventType, handlerMethod)
            } catch (e: Exception) {
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
     * 테스트용 속성 생성
     */
    fun createTestProperties(
        enabled: Boolean = true,
        async: Boolean = false,
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
        type: String = "TestEvent"
    ): TestDomainEvent {
        return TestDomainEvent(
            id = id,
            type = type,
            occurredAt = java.time.Instant.now()
        )
    }
}

