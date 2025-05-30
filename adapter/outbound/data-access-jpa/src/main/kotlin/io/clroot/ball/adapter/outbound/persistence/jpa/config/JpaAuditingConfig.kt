package io.clroot.ball.adapter.outbound.persistence.jpa.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.util.*

@Configuration
@EnableJpaAuditing(auditorAwareRef = "ballAuditorProvider")
class JpaAuditingConfig {

    @Bean
    @ConditionalOnMissingBean(name = ["ballAuditorProvider"])
    fun ballAuditorProvider(): AuditorAware<String> {
        return AuditorAware {
            Optional.of("system")
        }
    }
}