package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.clroot.ball.adapter.outbound.event.publisher.core.EventPublisherBase
import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.domain.event.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 도메인 이벤트 발행자
 *
 * 프로세스 내에서 도메인 이벤트를 처리하기 위한 발행자입니다.
 * Spring의 ApplicationEventPublisher를 사용하여 즉시 처리 또는 비동기 처리를 수행합니다.
 *
 * 사용 목적:
 * - 도메인 로직 내부의 이벤트 처리
 * - 같은 프로세스 내의 다른 핸들러들과의 통신
 * - 즉시 처리가 필요한 비즈니스 이벤트
 * - 개발/테스트 환경에서의 단순한 이벤트 처리
 *
 * 특징:
 * - 높은 성능 (메모리 내 처리)
 * - 트랜잭션 컨텍스트 공유
 * - JVM 종료 시 이벤트 손실 가능
 * - 단일 프로세스 제한
 */
@Component
class DomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val properties: DomainEventPublisherProperties
) : EventPublisherBase() {

    override fun beforePublish(event: DomainEvent) {
        if (properties.enableDebugLogging) {
            log.debug("[DOMAIN] Preparing to publish event: {} (ID: {})", event.type, event.id)
        }
        
        // 도메인 이벤트 유효성 검증
        validateDomainEvent(event)
    }

    override fun doPublish(event: DomainEvent) {
        if (properties.enableDebugLogging) {
            log.debug("[DOMAIN] Publishing domain event via ApplicationEventPublisher: {} (ID: {})", 
                event.type, event.id)
        }

        // Spring ApplicationEvent로 래핑하여 발행
        val wrapper = DomainEventWrapper(event)
        
        if (properties.async) {
            // 비동기 발행 (기본값)
            applicationEventPublisher.publishEvent(wrapper)
        } else {
            // 동기 발행 (디버깅용)
            applicationEventPublisher.publishEvent(wrapper)
        }
    }

    override fun afterPublish(event: DomainEvent) {
        if (properties.enableDebugLogging) {
            log.debug("[DOMAIN] Domain event published successfully: {} (ID: {})", event.type, event.id)
        }
        
        // 메트릭 수집
        recordPublishMetrics(event)
    }

    override fun handlePublishError(event: DomainEvent, error: Exception) {
        log.error("[DOMAIN] Failed to publish domain event: {} (ID: {})", event.type, event.id, error)
        
        // 도메인 이벤트 발행 실패는 비즈니스 로직에 영향을 줄 수 있으므로
        // 재시도 로직을 구현할 수 있음
        if (properties.enableRetry && canRetry(event, error)) {
            scheduleRetry(event)
        }
        
        // 메트릭 수집
        recordErrorMetrics(event, error)
    }

    /**
     * 도메인 이벤트 유효성 검증
     * 
     * 도메인 이벤트가 올바른 형태인지 검증합니다.
     */
    private fun validateDomainEvent(event: DomainEvent) {
        require(event.id.isNotBlank()) { 
            "Domain event ID cannot be blank: ${event.type}" 
        }
        require(event.type.isNotBlank()) { 
            "Domain event type cannot be blank: ${event.id}" 
        }
        require(event.occurredAt != null) { 
            "Domain event occurredAt cannot be null: ${event.type} (${event.id})" 
        }
    }

    /**
     * 재시도 가능 여부 판단
     */
    private fun canRetry(event: DomainEvent, error: Exception): Boolean {
        // 특정 예외 타입은 재시도하지 않음
        return when (error) {
            is IllegalArgumentException -> false
            is IllegalStateException -> false
            else -> true
        }
    }

    /**
     * 재시도 스케줄링
     * 
     * TODO: 실제 구현에서는 Spring의 @Retryable 또는 
     * 별도의 재시도 메커니즘을 사용할 수 있습니다.
     */
    private fun scheduleRetry(event: DomainEvent) {
        log.warn("[DOMAIN] Retry mechanism not implemented yet for event: {} (ID: {})", 
            event.type, event.id)
        // TODO: 실제 재시도 로직 구현
    }

    /**
     * 발행 성공 메트릭 기록
     */
    private fun recordPublishMetrics(event: DomainEvent) {
        if (properties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("domain.events.published")
            //     .tag("event.type", event.type)
            //     .increment()
        }
    }

    /**
     * 발행 실패 메트릭 기록
     */
    private fun recordErrorMetrics(event: DomainEvent, error: Exception) {
        if (properties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("domain.events.publish.errors")
            //     .tag("event.type", event.type)
            //     .tag("error.type", error.javaClass.simpleName)
            //     .increment()
        }
    }
}
