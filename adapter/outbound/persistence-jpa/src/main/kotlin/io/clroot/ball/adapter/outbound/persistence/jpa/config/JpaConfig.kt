package io.clroot.ball.adapter.outbound.persistence.jpa.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * JPA 설정
 */
@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = ["io.clroot.ball.adapter.outbound.persistence.jpa.entity"])
@EnableJpaRepositories(basePackages = ["io.clroot.ball.adapter.outbound.persistence.jpa.repository"])
class JpaConfig
