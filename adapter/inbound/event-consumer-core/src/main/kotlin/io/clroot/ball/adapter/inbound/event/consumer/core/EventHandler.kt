package io.clroot.ball.adapter.inbound.event.consumer.core

/**
 * 이벤트 핸들러 어노테이션
 *
 * ⚠️ **DEPRECATED**: 이 어노테이션은 헥사고날 아키텍처 원칙 위반으로 deprecated 됩니다.
 * 
 * **문제점:**
 * - 애플리케이션 계층이 어댑터 계층에 의존 (의존성 방향 위반)
 * - 어댑터가 비즈니스 로직 식별까지 담당 (단일 책임 원칙 위반)
 * 
 * **마이그레이션 방법:**
 * 
 * 1. **EventHandlerPort 구현 (권장)**:
 * ```kotlin
 * @Component
 * class UserCreatedEventHandler : EventHandlerPort<UserCreatedEvent> {
 *     override val eventType = UserCreatedEvent::class
 *     override suspend fun handle(event: UserCreatedEvent) {
 *         // 이벤트 처리 로직
 *     }
 * }
 * ```
 * 
 * 2. **@DomainEventHandler 어노테이션 사용 (호환성)**:
 * ```kotlin
 * @Component
 * class UserEventService {
 *     @DomainEventHandler
 *     suspend fun handleUserCreated(event: UserCreatedEvent) {
 *         // 이벤트 처리 로직  
 *     }
 * }
 * ```
 * 
 * **헥사고날 아키텍처 원칙:**
 * - 어댑터: 순수 기술적 연결 (이벤트 수신/전달)
 * - 애플리케이션: 비즈니스 로직 (이벤트 처리)
 * - 도메인: 핵심 비즈니스 규칙
 */
@Deprecated(
    message = "Violates Hexagonal Architecture principles. Use EventHandlerPort or @DomainEventHandler instead.",
    replaceWith = ReplaceWith(
        "DomainEventHandler", 
        "io.clroot.ball.application.event.DomainEventHandler"
    ),
    level = DeprecationLevel.ERROR
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler
