package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.clroot.ball.adapter.outbound.event.publisher.core.EventPublisherBase
import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.domain.event.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Spring ApplicationEvent 기반 도메인 이벤트 생산자
 *
 * Spring의 ApplicationEventPublisher를 사용하여 프로세스 내에서 도메인 이벤트를 발행합니다.
 * 주로 도메인 로직의 결과로 발생하는 이벤트들을 같은 프로세스 내의 핸들러들에게 전달합니다.
 *
 * 기술적 특징:
 * - Spring ApplicationEventPublisher 기반 (프로세스 내 메모리 처리)
 * - 높은 성능 (메모리 내 처리, 네트워크 오버헤드 없음)
 * - 트랜잭션 컨텍스트 공유
 * - JVM 종료 시 이벤트 손실 가능 (메모리 기반)
 * - 단일 프로세스 제한 (분산 처리 불가)
 *
 * vs 외부 메시징 시스템:
 * - SpringDomainEventProducer: 프로세스 내, 즉시 전달, 높은 성능
 * - KafkaEventProducer: 프로세스 간, 내구성, 확장성 (미래 구현)
 *
 * 사용 용도:
 * - 도메인 로직 내부의 이벤트 발행
 * - 같은 프로세스 내의 다른 컴포넌트와의 통신
 * - 즉시 처리가 필요한 비즈니스 이벤트
 * - 개발/테스트 환경에서의 단순한 이벤트 처리
 */
@Component
class SpringDomainEventProducer(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val properties: DomainEventPublisherProperties
) : EventPublisherBase() {

    override fun beforePublish(event: DomainEvent) {
        if (properties.enableDebugLogging) {
            log.debug("[SPRING] Preparing to publish domain event: {} (ID: {})", event.type, event.id)
        }

        // 도메인 이벤트 유효성 검증
        validateDomainEvent(event)
    }

    override fun doPublish(event: DomainEvent) {
        log.debug("[SPRING] Publishing domain event via ApplicationEventPublisher: {} (ID: {})", event.type, event.id)

        // Spring ApplicationEvent로 래핑하여 발행
        val wrapper = DomainEventWrapper(event)
        log.info("[SPRING] Created DomainEventWrapper: {}", wrapper)

        if (properties.async) {
            // 비동기 발행 (기본값)
            log.info("[SPRING] Publishing event ASYNC: {}", wrapper)
            applicationEventPublisher.publishEvent(wrapper)
        } else {
            // 동기 발행 (디버깅용)
            log.info("[SPRING] Publishing event SYNC: {}", wrapper)
            applicationEventPublisher.publishEvent(wrapper)
        }
        
        log.info("[SPRING] Event published to ApplicationEventPublisher: {}", wrapper)
    }

    override fun afterPublish(event: DomainEvent) {
        if (properties.enableDebugLogging) {
            log.debug("[SPRING] Domain event published successfully: {} (ID: {})", event.type, event.id)
        }

        // 메트릭 수집
        recordPublishMetrics(event)
    }

    override fun handlePublishError(event: DomainEvent, error: Exception) {
        log.error("[SPRING] Failed to publish domain event: {} (ID: {})", event.type, event.id, error)

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
        log.warn(
            "[SPRING] Retry mechanism not implemented yet for domain event: {} (ID: {})",
            event.type, event.id
        )
        // TODO: 실제 재시도 로직 구현
    }

    /**
     * 발행 성공 메트릭 기록
     */
    private fun recordPublishMetrics(event: DomainEvent) {
        if (properties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("spring.domain.events.published")
            //     .tag("event.type", event.type)
            //     .tag("producer", "spring")
            //     .increment()
        }
    }

    /**
     * 발행 실패 메트릭 기록
     */
    private fun recordErrorMetrics(event: DomainEvent, error: Exception) {
        if (properties.enableMetrics) {
            // TODO: Micrometer 메트릭 수집
            // counter("spring.domain.events.publish.errors")
            //     .tag("event.type", event.type)
            //     .tag("producer", "spring")
            //     .tag("error.type", error.javaClass.simpleName)
            //     .increment()
        }
    }
}
