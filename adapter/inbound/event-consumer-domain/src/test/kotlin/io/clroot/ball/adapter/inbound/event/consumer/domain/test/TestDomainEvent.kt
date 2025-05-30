package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.domain.event.DomainEvent

/**
 * 테스트용 도메인 이벤트
 */
data class TestDomainEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: java.time.Instant
) : DomainEvent