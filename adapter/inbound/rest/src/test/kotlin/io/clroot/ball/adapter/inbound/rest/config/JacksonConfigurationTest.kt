package io.clroot.ball.adapter.inbound.rest.config

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import java.time.LocalDateTime

/**
 * Jackson 설정이 제대로 적용되는지 테스트
 */
class JacksonConfigurationTest :
    BehaviorSpec({

        val contextRunner =
            WebApplicationContextRunner()
                .withConfiguration(
                    AutoConfigurations.of(
                        JacksonAutoConfiguration::class.java,
                        WebMvcAutoConfiguration::class.java,
                        RestAdapterAutoConfiguration::class.java,
                    ),
                )

        given("Jackson 설정이 적용된 환경에서") {
            `when`("ObjectMapper를 사용하면") {
                then("Ball Framework 설정이 적용되어야 한다") {
                    contextRunner.run { context ->
                        val objectMapper = context.getBean(ObjectMapper::class.java)

                        // 1. Kotlin 모듈 확인
                        val kotlinTestData = mapOf("name" to "test", "value" to 123)
                        val json = objectMapper.writeValueAsString(kotlinTestData)
                        val deserialized = objectMapper.readValue<Map<String, Any>>(json)
                        deserialized["name"] shouldBe "test"

                        // 2. Arrow Either 지원 확인
                        val either: Either<String, Int> = Either.Right(42)
                        val eitherJson = objectMapper.writeValueAsString(either)
                        val deserializedEither = objectMapper.readValue<Either<String, Int>>(eitherJson)
                        deserializedEither shouldBe either

                        // 3. Java Time 모듈 확인 (타임스탬프 비활성화)
                        val dateTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0)
                        val dateTimeJson = objectMapper.writeValueAsString(dateTime)
                        dateTimeJson shouldBe "\"2023-01-01T12:00:00\""

                        // 4. 설정 확인
                        objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) shouldBe false
                    }
                }
            }

            `when`("Jackson2ObjectMapperBuilderCustomizer가 있으면") {
                then("Ball Framework 커스터마이저가 등록되어야 한다") {
                    contextRunner.run { context ->
                        val customizers =
                            context.getBeansOfType(
                                org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer::class.java,
                            )

                        // Ball Framework 커스터마이저가 존재해야 함
                        customizers.containsKey("ballJacksonCustomizer") shouldBe true
                    }
                }
            }
        }
    })
