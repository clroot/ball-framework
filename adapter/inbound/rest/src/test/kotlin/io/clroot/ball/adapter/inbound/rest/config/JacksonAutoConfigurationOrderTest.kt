package io.clroot.ball.adapter.inbound.rest.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.core.annotation.Order

/**
 * Jackson AutoConfiguration 순서와 우선순위를 분석하는 테스트
 */
class JacksonAutoConfigurationOrderTest :
    BehaviorSpec({

        val contextRunner =
            WebApplicationContextRunner()
                .withConfiguration(
                    AutoConfigurations.of(
                        JacksonAutoConfiguration::class.java, // Spring Boot 기본 Jackson 설정
                        WebMvcAutoConfiguration::class.java,
                        RestAdapterAutoConfiguration::class.java, // Ball Framework Jackson 설정
                    ),
                )

        given("Spring Boot Jackson AutoConfiguration과 Ball Framework 설정이 함께 있을 때") {
            `when`("Application Context가 로드되면") {
                then("모든 Jackson2ObjectMapperBuilderCustomizer들이 등록되어야 한다") {
                    contextRunner.run { context ->
                        val customizers = context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer::class.java)

                        println("=== Jackson2ObjectMapperBuilderCustomizer 빈들 ===")
                        customizers.forEach { (name, customizer) ->
                            val orderAnnotation = customizer.javaClass.getAnnotation(Order::class.java)
                            val order = orderAnnotation?.value ?: Int.MAX_VALUE
                            val actualOrder =
                                if (customizer is org.springframework.core.Ordered) {
                                    customizer.order
                                } else {
                                    order
                                }
                            println("- $name: ${customizer.javaClass.simpleName} (Order annotation: $order, Actual order: $actualOrder)")
                        }

                        // Ball Framework 커스터마이저가 있어야 함
                        customizers.keys shouldContainAll listOf("ballJacksonCustomizer")
                    }
                }

                then("ObjectMapper가 적절히 설정되어야 한다") {
                    contextRunner.run { context ->
                        val objectMapper = context.getBean(ObjectMapper::class.java)

                        println("=== ObjectMapper 설정 상태 ===")
                        println(
                            "- WRITE_DATES_AS_TIMESTAMPS: ${objectMapper.serializationConfig.isEnabled(
                                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                            )}",
                        )
                        println(
                            "- FAIL_ON_UNKNOWN_PROPERTIES: ${objectMapper.deserializationConfig.isEnabled(
                                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            )}",
                        )
                        println("- Registered Modules: ${objectMapper.registeredModuleIds}")

                        // Ball Framework 설정이 적용되었는지 확인
                        objectMapper.serializationConfig.isEnabled(
                            com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        ) shouldBe
                            false
                        objectMapper.deserializationConfig.isEnabled(
                            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        ) shouldBe
                            false
                    }
                }
            }
        }

        given("RestAdapterAutoConfiguration만 있을 때") {
            val ballOnlyContextRunner =
                WebApplicationContextRunner()
                    .withConfiguration(
                        AutoConfigurations.of(
                            RestAdapterAutoConfiguration::class.java,
                        ),
                    )

            `when`("Application Context가 로드되면") {
                then("Ball Framework Jackson 설정만 적용되어야 한다") {
                    ballOnlyContextRunner.run { context ->
                        val customizers = context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer::class.java)

                        println("=== Ball Framework Only - Jackson2ObjectMapperBuilderCustomizer 빈들 ===")
                        customizers.forEach { (name, customizer) ->
                            println("- $name: ${customizer.javaClass.simpleName}")
                        }

                        customizers.size shouldBe 1
                        customizers.keys shouldContainAll listOf("ballJacksonCustomizer")
                    }
                }
            }
        }
    })
