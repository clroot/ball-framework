package io.clroot.ball.application.event

import io.clroot.ball.domain.event.DomainEvent

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