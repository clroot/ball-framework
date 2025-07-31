package io.clroot.ball.adapter.inbound.rest.config

import arrow.integrations.jackson.module.registerArrowModule
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
                // Java Time 모듈 등록 (JSR-310)
                registerModule(JavaTimeModule())

                // Kotlin 모듈 등록
                registerKotlinModule()

                // Arrow 함수형 프로그래밍 모듈 등록
                registerArrowModule()

                // 타임스탬프 대신 ISO 형식 사용
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

                // 알 수 없는 프로퍼티에 대해 실패하지 않음
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

                // null 값은 JSON에 포함하지 않음
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
            }
        }
    }

    override fun getOrder(): Int = 10
}
