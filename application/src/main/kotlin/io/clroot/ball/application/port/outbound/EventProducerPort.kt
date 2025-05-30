package io.clroot.ball.application.port.outbound

import io.clroot.ball.domain.event.Event

/**
 * 이벤트 생산 포트 (Event Producer Port)
 *
 * 이벤트를 발행하고 배포하는 퍼블리셔의 계약을 정의합니다.
 * 헥사고날 아키텍처에서 애플리케이션 계층의 출구점(Outbound Port) 역할을 합니다.
 *
 * 범용 설계:
 * - Event 기반으로 DomainEvent, IntegrationEvent 모두 처리 가능
 * - 어댑터 계층에서 구체적인 메시징 기술과 연결
 * - 동기/비동기 처리는 구현체에서 결정
 *
 * 사용 예시:
 * ```kotlin
 * @Service
 * class UserService(
 *     private val eventProducer: EventProducerPort
 * ) {
 *     suspend fun createUser(request: CreateUserRequest) {
 *         // 비즈니스 로직...
 *         eventProducer.produce(UserCreatedEvent(userId, email))
 *     }
 * }
 * ```
 */
interface EventProducerPort {

    /**
     * 단일 이벤트 발행
     *
     * @param event 발행할 이벤트
     */
    suspend fun produce(event: Event)

    /**
     * 여러 이벤트 일괄 발행
     *
     * 기본 구현은 개별 발행을 순차 처리하지만,
     * 구현체에서 배치 처리로 최적화할 수 있습니다.
     *
     * @param events 발행할 이벤트 목록
     */
    suspend fun produce(events: List<Event>) {
        events.forEach { produce(it) }
    }
}