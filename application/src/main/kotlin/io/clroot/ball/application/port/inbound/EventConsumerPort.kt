package io.clroot.ball.application.port.inbound

import io.clroot.ball.domain.event.Event
import kotlin.reflect.KClass

/**
 * 이벤트 소비 포트 (Event Consumer Port)
 *
 * 이벤트를 수신하고 처리하는 핸들러의 계약을 정의합니다.
 * 헥사고날 아키텍처에서 애플리케이션 계층의 진입점(Inbound Port) 역할을 합니다.
 *
 * 범용 설계:
 * - Event 기반으로 DomainEvent, IntegrationEvent 모두 처리 가능
 * - 제네릭을 통한 타입 안전성 보장
 * - 어댑터 계층에서 구체적인 메시징 기술과 연결
 *
 * 사용 예시:
 * ```kotlin
 * @Component
 * class UserCreatedEventHandler : EventConsumerPort<UserCreatedEvent> {
 *     override val eventType = UserCreatedEvent::class
 *     override suspend fun consume(event: UserCreatedEvent) {
 *         // 이벤트 처리 로직
 *     }
 * }
 * ```
 *
 * @param T 처리할 이벤트 타입 (Event의 하위 타입)
 */
interface EventConsumerPort<T : Event> {

    /**
     * 처리할 이벤트 타입
     */
    val eventType: KClass<T>

    /**
     * 이벤트 소비 (처리)
     *
     * @param event 처리할 이벤트
     */
    suspend fun consume(event: T)

    /**
     * 핸들러 실행 순서 (낮을수록 먼저 실행)
     */
    val order: Int get() = 0

    /**
     * 비동기 처리 여부
     */
    val async: Boolean get() = true

    /**
     * 핸들러 이름 (로깅/디버깅용)
     */
    val handlerName: String get() = javaClass.simpleName
}