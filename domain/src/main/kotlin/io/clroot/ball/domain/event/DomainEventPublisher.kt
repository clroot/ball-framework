package io.clroot.ball.domain.event

interface DomainEventPublisher {
    fun publish(event: DomainEvent)

    fun publish(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
