package io.clroot.ball.application.event

import io.clroot.ball.domain.event.DomainEvent

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
