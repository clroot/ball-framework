package io.clroot.ball.domain.event

import java.time.Instant

interface DomainEvent {
    val occurredAt: Instant
}
