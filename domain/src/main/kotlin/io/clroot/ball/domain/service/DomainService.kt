package io.clroot.ball.domain.service

import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.event.DomainEventPublisher

/**
 * 도메인 서비스 (Domain Service)
 *
 * 여러 집합체에 걸친 비즈니스 로직을 캡슐화하는 서비스
 * 특정 엔티티에 속하지 않는 도메인 로직을 구현
 */
interface DomainService

/**
 * 도메인 이벤트 핸들러 (Domain Event Handler)
 *
 * 도메인 이벤트를 처리하는 핸들러 인터페이스
 *
 * @param T 처리할 도메인 이벤트 타입
 */
interface DomainEventHandler<T : DomainEvent> {
    /**
     * 도메인 이벤트 처리
     *
     * @param event 처리할 도메인 이벤트
     */
    suspend fun handle(event: T)
}

/**
 * 도메인 이벤트 디스패처 (Domain Event Dispatcher)
 *
 * 도메인 이벤트를 적절한 핸들러에 전달하는 디스패처
 */
interface DomainEventDispatcher {
    /**
     * 도메인 이벤트 핸들러 등록
     *
     * @param handler 등록할 도메인 이벤트 핸들러
     * @param eventType 처리할 도메인 이벤트 타입
     */
    fun <T : DomainEvent> registerHandler(handler: DomainEventHandler<T>, eventType: Class<T>)
    
    /**
     * 도메인 이벤트 디스패치
     *
     * @param event 디스패치할 도메인 이벤트
     */
    suspend fun dispatch(event: DomainEvent)
    
    /**
     * 도메인 이벤트 목록 디스패치
     *
     * @param events 디스패치할 도메인 이벤트 목록
     */
    suspend fun dispatchAll(events: List<DomainEvent>) {
        events.forEach { dispatch(it) }
    }
}

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