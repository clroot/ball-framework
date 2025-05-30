package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.clroot.ball.domain.event.DomainEvent
import java.time.Instant

/**
 * 테스트용 도메인 이벤트
 */
data class TestDomainEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant
) : DomainEvent

/**
 * 복잡한 도메인 이벤트 테스트용 클래스
 */
data class ComplexDomainEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant,
    val orderId: String,
    val customerId: String,
    val totalAmount: Int,
    val items: List<String>
) : DomainEvent

/**
 * DomainEvent가 아닌 Event 구현체 (지원하지 않는 타입 테스트용)
 */
data class UnsupportedTestEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant
) : io.clroot.ball.domain.event.Event
