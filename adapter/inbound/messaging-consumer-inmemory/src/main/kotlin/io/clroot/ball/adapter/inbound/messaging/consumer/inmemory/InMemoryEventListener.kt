package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.inbound.messaging.consumer.core.executor.DomainEventHandlerExecutor
import io.clroot.ball.adapter.inbound.messaging.consumer.core.listener.AbstractEventListener
import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.domain.event.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async

/**
 * 인메모리 도메인 이벤트 리스너
 *
 * Spring ApplicationEvent로 발행된 도메인 이벤트를 수신하고,
 * core 모듈의 공통 로직을 사용하여 등록된 핸들러들에게 전달합니다.
 *
 * 주요 특징:
 * - Spring ApplicationEvent 기반 이벤트 수신
 * - AbstractEventListener 상속으로 공통 로직 활용
 * - 순환 호출 방지 (DomainEventDispatcher 거치지 않음)
 * - 비동기 처리 지원
 *
 * 이 클래스는 Auto Configuration에 의해 자동으로 등록됩니다.
 */
open class InMemoryEventListener(
    handlerExecutor: DomainEventHandlerExecutor,
    private val inMemoryProperties: InMemoryEventConsumerProperties
) : AbstractEventListener(handlerExecutor, inMemoryProperties) {
    
    private val inMemoryLog = LoggerFactory.getLogger(javaClass)

    /**
     * Spring ApplicationEvent로 발행된 도메인 이벤트 처리
     * 
     * @EventListener를 통해 DomainEventWrapper를 수신하고,
     * 부모 클래스의 공통 로직을 사용하여 처리합니다.
     */
    @EventListener
    @Async("eventTaskExecutor")
    open fun handleDomainEvent(wrapper: DomainEventWrapper) {
        val event = wrapper.domainEvent
        
        if (inMemoryProperties.enableDebugLogging) {
            inMemoryLog.debug("InMemory consumer received domain event: {} (ID: {})", event.type, event.id)
        }

        // 부모 클래스의 공통 처리 로직 호출
        processEvent(event)
    }

    /**
     * InMemory 전용 에러 핸들링
     * 
     * 부모 클래스의 기본 에러 처리를 확장합니다.
     */
    override fun handleEventError(event: DomainEvent, error: Exception) {
        if (inMemoryProperties.enableDebugLogging) {
            inMemoryLog.debug("InMemory specific error handling for event {} (ID: {})", event.type, event.id)
        }

        // 부모 클래스의 기본 에러 처리 호출
        super.handleEventError(event, error)

        // InMemory 전용 에러 처리 로직 (필요시 추가)
        // 예: InMemory 전용 메트릭 수집, 로컬 파일 로깅 등
    }
}
