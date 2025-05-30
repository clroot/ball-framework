package io.clroot.ball.adapter.inbound.event.consumer.core

import io.clroot.ball.domain.event.DomainEvent

/**
 * 이벤트 핸들러 레지스트리 인터페이스
 *
 * 프로덕션 코드와 테스트 코드에서 공통으로 사용할 수 있는 계약을 정의합니다.
 * 테스트 가능한 설계를 위해 의존성 역전 원칙을 적용합니다.
 */
interface EventHandlerRegistryInterface {

    /**
     * 특정 이벤트 타입에 대한 핸들러들 반환
     */
    fun getHandlers(eventType: Class<out DomainEvent>): List<EventHandlerMethod>

    /**
     * 모든 핸들러가 처리하는 이벤트 타입들 반환
     */
    fun getAllHandledEventTypes(): Set<Class<out DomainEvent>>

    /**
     * 핸들러 등록
     */
    fun registerHandler(eventType: Class<out DomainEvent>, handler: EventHandlerMethod)

    /**
     * 핸들러 제거
     */
    fun unregisterHandler(eventType: Class<out DomainEvent>, handler: EventHandlerMethod)
}
