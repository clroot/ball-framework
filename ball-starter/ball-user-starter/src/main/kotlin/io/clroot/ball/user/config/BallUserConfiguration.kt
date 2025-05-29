package io.clroot.ball.user.config

import io.clroot.ball.adapter.outbound.persistence.jpa.config.BallPersistenceJpaConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan(
    basePackages = [
        "io.clroot.ball.user",

    ]
)
@EntityScan(
    basePackages = [
        "io.clroot.ball.domain.model.core",
        "io.clroot.ball.adapter.outbound.persistence.jpa.converter",
        "io.clroot.ball.user.adapter.outbound.persistence.jpa"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "io.clroot.ball.user.adapter.outbound.persistence.jpa"
    ]
)
@Import(
    BallPersistenceJpaConfiguration::class
)
class BallUserConfiguration {
    init {
        println("BallUserConfiguration loaded")
    }
}