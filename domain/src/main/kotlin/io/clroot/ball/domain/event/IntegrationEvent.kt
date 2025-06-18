package io.clroot.ball.domain.event

import io.clroot.ball.domain.model.vo.BinaryId
import java.time.LocalDateTime

/**
 * 통합 이벤트 (Integration Event)
 *
 * 서비스 간 통신을 위한 이벤트입니다.
 *
 * 특징:
 * - 서비스 경계를 넘나드는 통신
 * - 메시징 시스템 (Kafka, RabbitMQ 등) 사용
 * - 비동기 처리, 내구성 보장
 * - 네트워크를 통한 전송 고려
 *
 * vs DomainEvent:
 * - DomainEvent: 같은 서비스 내 컴포넌트 간 통신 (프로세스 내)
 * - IntegrationEvent: 서로 다른 서비스 간 통신 (프로세스 간)
 *
 * 사용 예시:
 * ```kotlin
 * data class OrderCompletedIntegrationEvent(
 *     override val id: String,
 *     override val source: String = "order-service",
 *     override val destination: String = "order-completed-topic",
 *     val orderId: String,
 *     val customerId: String
 * ) : IntegrationEvent
 * ```
 */
interface IntegrationEvent : Event {
    /**
     * 이벤트 발생 소스 (서비스명, 시스템명 등)
     */
    val source: String

    /**
     * 목적지 (토픽, 큐, 라우팅 키 등)
     * null인 경우 기본 라우팅 규칙 적용
     */
    val destination: String?

    /**
     * 연관 ID (트레이싱, 사가 패턴 등에서 사용)
     */
    val correlationId: String?

    /**
     * 추가 메타데이터
     * - 헤더 정보
     * - 라우팅 정보
     * - 스키마 버전 등
     */
    val metadata: Map<String, Any>

    /**
     * 이벤트 타입
     * 기본적으로 클래스 이름을 사용
     */
    override val type: String get() = this::class.simpleName ?: "UnknownIntegrationEvent"
}

/**
 * 통합 이벤트 기본 구현
 */
abstract class IntegrationEventBase(
    override val source: String,
    override val destination: String? = null,
    override val correlationId: String? = null,
    override val metadata: Map<String, Any> = emptyMap(),
) : IntegrationEvent {
    override val id: String = BinaryId.new().toString()
    override val occurredAt: LocalDateTime = LocalDateTime.now()
}
