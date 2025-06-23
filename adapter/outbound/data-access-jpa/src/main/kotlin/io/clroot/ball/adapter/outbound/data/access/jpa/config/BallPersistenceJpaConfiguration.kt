package io.clroot.ball.adapter.outbound.data.access.jpa.config

import io.clroot.ball.adapter.outbound.data.access.jpa.converter.DurationConverter
import io.clroot.ball.adapter.outbound.data.access.jpa.converter.LocalDateTimeConverter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    DurationConverter::class,
    LocalDateTimeConverter::class,
)
class BallPersistenceJpaConfiguration
