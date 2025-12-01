package io.clroot.ball.application

import io.clroot.ball.application.warmup.WarmupHealthIndicator
import io.clroot.ball.application.warmup.WarmupService
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableAsync

@AutoConfiguration
@EnableResilientMethods
@EnableAsync
@EnableConfigurationProperties(ApplicationLayerProperties::class)
@Import(
    WarmupService::class,
    WarmupHealthIndicator::class,
)
class ApplicationLayerAutoConfiguration {
    @Bean
    @ConditionalOnProperty(
        prefix = "ball.application",
        name = ["warmup-enabled"],
        havingValue = "true",
        matchIfMissing = false,
    )
    fun warmupRunner(
        warmupService: WarmupService,
        properties: ApplicationLayerProperties,
    ): ApplicationRunner =
        ApplicationRunner {
            warmupService.performWarmup(properties.warmupTimeoutSeconds)
        }
}
