package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.shared.jackson.registerBallModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * Ball Framework Jackson ObjectMapper 커스터마이저
 *
 * Spring Boot의 기본 Jackson 설정 이후에 실행되도록 Order 값을 설정합니다.
 * 기본 standardJacksonObjectMapperBuilderCustomizer의 Order는 0이므로,
 * 그보다 높은 값(10)을 설정하여 Ball Framework 설정이 나중에 적용되도록 합니다.
 */
@Order(10)
class BallJackson2ObjectMapperBuilderCustomizer :
    Jackson2ObjectMapperBuilderCustomizer,
    Ordered {
    override fun customize(jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder) {
        jacksonObjectMapperBuilder.postConfigurer { objectMapper ->
            objectMapper.apply {
                registerBallModule()
            }
        }
    }

    override fun getOrder(): Int = 10
}
