package io.clroot.ball.domain.event

import java.time.LocalDateTime

/**
 * 기본 이벤트 인터페이스
 *
 * 모든 이벤트의 최상위 인터페이스입니다.
 * DomainEvent와 IntegrationEvent의 공통 계약을 정의합니다.
 */
interface Event {
    /**
     * 이벤트 고유 식별자
     */
    val id: String

    /**
     * 이벤트 타입 (예: "UserCreated", "OrderProcessed")
     */
    val type: String

    /**
     * 이벤트 발생 시간
     */
    val occurredAt: LocalDateTime
}
