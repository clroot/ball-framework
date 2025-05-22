package io.clroot.ball.application.event

import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEvent

/**
 * 기본 도메인 이벤트 디스패처 구현
 *
 * @param eventPublisher 도메인 이벤트 발행자
 */
abstract class AbstractDomainEventDispatcher(
    private val eventPublisher: DomainEventPublisher
) : DomainEventDispatcher {
    private val handlers = mutableMapOf<Class<out DomainEvent>, MutableList<DomainEventHandler<*>>>()

    override fun <T : DomainEvent> registerHandler(handler: DomainEventHandler<T>, eventType: Class<T>) {
        val eventHandlers = handlers.getOrPut(eventType) { mutableListOf() }
        eventHandlers.add(handler)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun dispatch(event: DomainEvent) {
        // 이벤트 발행
        eventPublisher.publish(event)

        // 등록된 핸들러에 이벤트 전달
        val eventType = event::class.java
        handlers[eventType]?.forEach { handler ->
            (handler as DomainEventHandler<DomainEvent>).handle(event)
        }
    }
}