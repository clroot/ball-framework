package io.clroot.ball.adapter.outbound.data.access.jpa.config

import io.clroot.ball.adapter.outbound.data.access.jpa.converter.BinaryIdConverter
import io.clroot.ball.adapter.outbound.data.access.jpa.converter.DurationConverter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    BinaryIdConverter::class,
    DurationConverter::class,
    io.clroot.ball.adapter.outbound.data.access.jpa.converter.LocalDateTimeConverter::class,
)
class BallPersistenceJpaConfiguration
