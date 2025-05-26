package io.clroot.ball.adapter.shared.messaging

import io.clroot.ball.domain.event.DomainEvent
import org.springframework.context.ApplicationEvent

/**
 * Spring ApplicationEvent 시스템과 통합하기 위한 도메인 이벤트 래퍼
 * 
 * 도메인 이벤트를 Spring의 이벤트 시스템에서 처리할 수 있도록 래핑합니다.
 * 이를 통해 @EventListener를 사용한 이벤트 처리가 가능해집니다.
 * 
 * 이 클래스는 InMemory Producer와 Consumer 모두에서 공통으로 사용됩니다.
 */
class DomainEventWrapper(
    /**
     * 래핑된 도메인 이벤트
     */
    val domainEvent: DomainEvent
) : ApplicationEvent(domainEvent) {

    /**
     * 이벤트 타입 반환 (로깅 및 디버깅용)
     */
    val eventType: String
        get() = domainEvent.type

    /**
     * 이벤트 ID 반환
     */
    val eventId: String
        get() = domainEvent.id

    override fun toString(): String {
        return "DomainEventWrapper(eventType='$eventType', eventId='$eventId')"
    }
}
