package io.clroot.ball.adapter.inbound.event.consumer.domain.test

import io.clroot.ball.domain.event.DomainEvent

/**
 * 테스트용 도메인 이벤트
 */
data class TestDomainEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: java.time.Instant,
    val message: String = "Default test message"
) : DomainEvent {
    
    companion object {
        fun create(
            id: String = "test-${System.currentTimeMillis()}",
            type: String = "TestEvent",
            message: String = "Test message"
        ): TestDomainEvent {
            return TestDomainEvent(
                id = id,
                type = type,
                occurredAt = java.time.Instant.now(),
                message = message
            )
        }
    }
}
