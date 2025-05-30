package io.clroot.ball.adapter.outbound.persistence.jpa.config

import io.clroot.ball.adapter.outbound.persistence.jpa.converter.BinaryIdConverter
import io.clroot.ball.adapter.outbound.persistence.jpa.converter.DurationConverter
import io.clroot.ball.adapter.outbound.persistence.jpa.converter.InstantConverter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    BinaryIdConverter::class,
    DurationConverter::class,
    InstantConverter::class,
)
class BallPersistenceJpaConfiguration