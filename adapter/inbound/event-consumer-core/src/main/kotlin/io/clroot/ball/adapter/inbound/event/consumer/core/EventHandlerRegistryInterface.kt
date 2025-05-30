package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.domain.event.DomainEvent

/**
 * ThreadPool 기반 이벤트 핸들러 레지스트리 인터페이스
 * 
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 */
interface EventHandlerRegistryInterface {
    
    /**
     * 특정 이벤트 타입에 대한 ThreadPool 핸들러들 반환
     */
    fun getHandlers(eventType: Class<out DomainEvent>): List<ThreadPoolEventHandlerMethod>
    
    /**
     * 모든 핸들러가 처리하는 이벤트 타입들 반환
     */
    fun getAllHandledEventTypes(): Set<Class<out DomainEvent>>
    
    /**
     * ThreadPool 핸들러 등록
     */
    fun registerHandler(eventType: Class<out DomainEvent>, handler: ThreadPoolEventHandlerMethod)
    
    /**
     * ThreadPool 핸들러 제거
     */
    fun unregisterHandler(eventType: Class<out DomainEvent>, handler: ThreadPoolEventHandlerMethod)
}
