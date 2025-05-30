package io.clroot.ball.application.port

import io.clroot.ball.application.port.inbound.EventConsumerPort
import io.clroot.ball.application.port.outbound.EventProducerPort
import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.event.IntegrationEvent

/**
 * 이벤트 포트 특화 인터페이스
 * 
 * 범용 EventConsumerPort/EventProducerPort를 도메인별로 특화한 인터페이스입니다.
 * typealias 대신 인터페이스 상속을 사용하여 타입 안전성을 보장합니다.
 */

// ==================== 도메인 이벤트 특화 (현재 주로 사용) ====================

/**
 * 도메인 이벤트 소비 포트
 * 
 * 같은 프로세스 내에서 발생하는 도메인 이벤트를 처리합니다.
 * EventConsumerPort<DomainEvent>를 상속하여 타입 안전성을 보장합니다.
 * 
 * 사용 예시:
 * ```kotlin
 * @Component
 * class UserCreatedEventHandler : DomainEventConsumerPort<UserCreatedEvent> {
 *     override val eventType = UserCreatedEvent::class
 *     override suspend fun consume(event: UserCreatedEvent) {
 *         // 도메인 로직 처리
 *     }
 * }
 * ```
 */
interface DomainEventConsumerPort<T : DomainEvent> : EventConsumerPort<T>

/**
 * 도메인 이벤트 생산 포트
 * 
 * 도메인 로직의 결과로 도메인 이벤트를 발행합니다.
 * 
 * 사용 예시:
 * ```kotlin
 * @Service
 * class UserService(
 *     private val eventProducer: DomainEventProducerPort
 * ) {
 *     suspend fun createUser(request: CreateUserRequest) {
 *         // 비즈니스 로직...
 *         eventProducer.produce(UserCreatedEvent(userId, email))
 *     }
 * }
 * ```
 */
interface DomainEventProducerPort : EventProducerPort

// ==================== 통합 이벤트 특화 (미래 확장) ====================

/**
 * 통합 이벤트 소비 포트
 * 
 * 외부 시스템으로부터 수신하는 통합 이벤트를 처리합니다.
 * 
 * 사용 예시:
 * ```kotlin
 * @Component
 * class OrderCompletedIntegrationEventHandler : IntegrationEventConsumerPort<OrderCompletedIntegrationEvent> {
 *     override val eventType = OrderCompletedIntegrationEvent::class
 *     override suspend fun consume(event: OrderCompletedIntegrationEvent) {
 *         // 외부 시스템 연동 로직
 *     }
 * }
 * ```
 */
interface IntegrationEventConsumerPort<T : IntegrationEvent> : EventConsumerPort<T>

/**
 * 통합 이벤트 생산 포트
 * 
 * 외부 시스템으로 통합 이벤트를 발행합니다.
 * 
 * 사용 예시:
 * ```kotlin
 * @Service
 * class OrderService(
 *     private val integrationEventProducer: IntegrationEventProducerPort
 * ) {
 *     suspend fun completeOrder(orderId: String) {
 *         // 비즈니스 로직...
 *         integrationEventProducer.produce(
 *             OrderCompletedIntegrationEvent(
 *                 orderId = orderId,
 *                 source = "order-service",
 *                 destination = "order-completed-topic"
 *             )
 *         )
 *     }
 * }
 * ```
 */
interface IntegrationEventProducerPort : EventProducerPort

// ==================== 레거시 호환성 ====================

/**
 * @deprecated DomainEventConsumerPort를 사용하세요.
 */
@Deprecated(
    message = "Use DomainEventConsumerPort instead",
    replaceWith = ReplaceWith("DomainEventConsumerPort<T>"),
    level = DeprecationLevel.WARNING
)
interface EventHandlerPort<T : DomainEvent> : DomainEventConsumerPort<T>
