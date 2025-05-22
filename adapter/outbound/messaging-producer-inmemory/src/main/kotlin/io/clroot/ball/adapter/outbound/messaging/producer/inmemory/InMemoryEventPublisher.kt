package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.application.event.DomainEventDispatcher
import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 인메모리 도메인 이벤트 발행자
 * 
 * 단일 프로세스 내에서 도메인 이벤트를 발행하고 처리하는 기본 구현체입니다.
 * 외부 메시징 시스템 없이도 이벤트 기반 아키텍처를 구현할 수 있습니다.
 */
@Component
@ConditionalOnProperty(
    name = ["ball.event.publisher.type"],
    havingValue = "inmemory",
    matchIfMissing = true  // 기본값으로 사용
)
class InMemoryEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val domainEventDispatcher: DomainEventDispatcher,
    private val properties: InMemoryEventPublisherProperties
) : DomainEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)
    private val eventScope = CoroutineScope(Dispatchers.Default)

    override fun publish(event: DomainEvent) {
        log.debug("Publishing event in-memory: {}", event.javaClass.simpleName)

        try {
            if (properties.async) {
                publishAsync(event)
            } else {
                publishSync(event)
            }
            
            log.debug("Event published successfully: {}", event.javaClass.simpleName)
        } catch (e: Exception) {
            log.error("Failed to publish event: {}", event.javaClass.simpleName, e)
            handlePublishError(event, e)
        }
    }

    /**
     * 동기적으로 이벤트 처리
     */
    private fun publishSync(event: DomainEvent) {
        runBlocking {
            domainEventDispatcher.dispatch(event)
        }
        
        // Spring ApplicationEvent로도 발행 (다른 리스너들을 위해)
        applicationEventPublisher.publishEvent(DomainEventWrapper(event))
    }

    /**
     * 비동기적으로 이벤트 처리
     */
    private fun publishAsync(event: DomainEvent) {
        eventScope.launch {
            try {
                domainEventDispatcher.dispatch(event)
            } catch (e: Exception) {
                log.error("Failed to dispatch event asynchronously: {}", event.javaClass.simpleName, e)
                handlePublishError(event, e)
            }
        }
        
        // Spring ApplicationEvent로도 발행
        applicationEventPublisher.publishEvent(DomainEventWrapper(event))
    }

    /**
     * 이벤트 발행 오류 처리
     */
    private fun handlePublishError(event: DomainEvent, error: Exception) {
        log.error("Error handling for event: {}", event.javaClass.simpleName, error)
        
        if (properties.enableRetry) {
            // 재시도 로직은 별도 컴포넌트에서 처리하거나
            // 여기서 간단한 재시도 구현 가능
            log.warn("Retry mechanism not implemented yet for event: {}", event.javaClass.simpleName)
        }
    }
}
