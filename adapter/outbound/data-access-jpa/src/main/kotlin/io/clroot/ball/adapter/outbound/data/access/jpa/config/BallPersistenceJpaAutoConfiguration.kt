package io.clroot.ball.adapter.outbound.data.access.jpa.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import

@AutoConfiguration
@ConditionalOnMissingBean(BallPersistenceJpaConfiguration::class)
@Import(
    JpaAuditingConfig::class,
    BallPersistenceJpaConfiguration::class
)
class BallPersistenceJpaAutoConfiguration