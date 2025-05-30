package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethod
import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerRegistryInterface
import io.clroot.ball.domain.event.DomainEvent

/**
 * 테스트용 EventHandlerRegistry 구현체
 *
 * ApplicationContext 의존성 없이 동작하는 단순한 레지스트리
 */
class TestEventHandlerRegistry : EventHandlerRegistryInterface {

    // 이벤트 타입별 핸들러 매핑
    private val handlerMap = mutableMapOf<Class<out DomainEvent>, MutableList<EventHandlerMethod>>()

    /**
     * 특정 이벤트 타입에 대한 핸들러들 반환
     */
    override fun getHandlers(eventType: Class<out DomainEvent>): List<EventHandlerMethod> {
        return handlerMap[eventType]?.toList() ?: emptyList()
    }

    /**
     * 핸들러 등록
     */
    override fun registerHandler(eventType: Class<out DomainEvent>, handler: EventHandlerMethod) {
        handlerMap.computeIfAbsent(eventType) { mutableListOf() }.add(handler)
    }

    /**
     * 핸들러 제거
     */
    override fun unregisterHandler(eventType: Class<out DomainEvent>, handler: EventHandlerMethod) {
        handlerMap[eventType]?.remove(handler)
    }

    /**
     * 모든 핸들러가 처리하는 이벤트 타입들 반환
     */
    override fun getAllHandledEventTypes(): Set<Class<out DomainEvent>> {
        return handlerMap.keys.toSet()
    }
}