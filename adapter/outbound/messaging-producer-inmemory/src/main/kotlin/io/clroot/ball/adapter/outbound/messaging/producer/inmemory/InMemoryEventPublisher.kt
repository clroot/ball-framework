package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher

/**
 * 인메모리 도메인 이벤트 발행자
 *
 * 단일 프로세스 내에서 도메인 이벤트를 발행하는 구현체입니다.
 *
 * 책임:
 * - 도메인 이벤트를 ApplicationEvent로 변환하여 발행
 * - 이벤트 처리는 Consumer(InMemoryEventListener)에서 담당
 *
 * 이 클래스는 Auto Configuration에 의해 자동으로 등록됩니다.
 */
class InMemoryEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val properties: InMemoryEventPublisherProperties
) : DomainEventPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: DomainEvent) {
        log.debug("Publishing event in-memory: {}", event.javaClass.simpleName)
        try {
            // Spring ApplicationEvent로 발행 (Consumer가 처리)
            applicationEventPublisher.publishEvent(DomainEventWrapper(event))
            log.debug("Event published successfully: {}", event.javaClass.simpleName)
        } catch (e: Exception) {
            log.error("Failed to publish event: {}", event.javaClass.simpleName, e)
            handlePublishError(event, e)
        }
    }

    /**
     * 이벤트 발행 오류 처리
     */
    private fun handlePublishError(event: DomainEvent, error: Exception) {
        log.error("Error handling for event: {}", event.javaClass.simpleName, error)
        if (properties.enableRetry) {
            log.warn("Retry mechanism not implemented yet for event: {}", event.javaClass.simpleName)
            // TODO: 재시도 로직 구현 (필요시)
        }
    }
}
