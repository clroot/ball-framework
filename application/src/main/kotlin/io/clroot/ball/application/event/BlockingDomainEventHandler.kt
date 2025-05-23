package io.clroot.ball.application.event

import io.clroot.ball.domain.event.DomainEvent

/**
 * Blocking Domain Event Handler
 * 
 * JPA, JDBC 등 blocking I/O를 사용하는 핸들러를 위한 인터페이스입니다.
 * Spring Boot + JPA 환경에서 주로 사용됩니다.
 * 
 * 특징:
 * - 일반 함수 (suspend function 아님)
 * - Spring @Transactional 지원
 * - Spring @Async 스레드 풀에서 실행
 * - JPA Repository 등 blocking I/O 안전하게 사용 가능
 * 
 * @param T 처리할 도메인 이벤트 타입
 */
interface BlockingDomainEventHandler<T : DomainEvent> {
    /**
     * 도메인 이벤트 처리 (Blocking)
     * 
     * 이 메서드는 Spring의 @Async 스레드에서 실행되므로
     * JPA Repository, JDBC, HTTP 호출 등 blocking I/O를 안전하게 사용할 수 있습니다.
     * 
     * @param event 처리할 도메인 이벤트
     */
    fun handle(event: T)
}
