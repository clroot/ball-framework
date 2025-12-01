package io.clroot.ball.adapter.inbound.rest.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

/**
 * Ball Framework Jackson ObjectMapper 커스터마이저
 *
 */
@Order(10)
@Configuration
class BallJacksonConfiguration {
    @Bean
    fun jacksonCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder
                .addModule(
                    KotlinModule
                        .Builder()
                        .configure(KotlinFeature.NullToEmptyCollection, false)
                        .configure(KotlinFeature.NullToEmptyMap, false)
                        .configure(KotlinFeature.NullIsSameAsDefault, false)
                        .configure(KotlinFeature.SingletonSupport, true)
                        .configure(KotlinFeature.StrictNullChecks, false)
                        .build(),
                ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
}
