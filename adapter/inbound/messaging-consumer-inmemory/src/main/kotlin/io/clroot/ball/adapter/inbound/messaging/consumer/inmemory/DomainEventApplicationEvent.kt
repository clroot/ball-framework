package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.domain.event.DomainEvent
import org.springframework.context.ApplicationEvent

/**
 * Consumer 전용 도메인 이벤트 래퍼
 * 
 * Producer와 독립적으로 동작하는 Consumer 전용 ApplicationEvent입니다.
 * Producer가 없어도 테스트와 개발이 가능합니다.
 */
class DomainEventApplicationEvent(
    val domainEvent: DomainEvent
) : ApplicationEvent(domainEvent) {
    
    val eventType: String get() = domainEvent.type
    val eventId: String get() = domainEvent.id
    
    override fun toString(): String {
        return "DomainEventApplicationEvent(eventType='$eventType', eventId='$eventId')"
    }
}
