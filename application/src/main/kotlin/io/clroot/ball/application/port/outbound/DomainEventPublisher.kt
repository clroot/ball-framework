package io.clroot.ball.application.port.outbound

import io.clroot.ball.domain.event.DomainEvent

interface DomainEventPublisher {
    fun publish(event: DomainEvent)

    fun publish(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
