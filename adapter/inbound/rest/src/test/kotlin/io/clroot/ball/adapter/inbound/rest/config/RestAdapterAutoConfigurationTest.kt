package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.filter.RequestLoggingFilter
import io.clroot.ball.adapter.inbound.rest.handler.GlobalExceptionHandler
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

/**
 * 기본 Auto-Configuration 테스트
 */
class RestAdapterAutoConfigurationTest :
    BehaviorSpec({

        val contextRunner =
            WebApplicationContextRunner()
                .withConfiguration(
                    AutoConfigurations.of(
                        WebMvcAutoConfiguration::class.java,
                        RestAdapterAutoConfiguration::class.java,
                    ),
                )

        given("웹 애플리케이션 환경에서") {
            `when`("기본 설정으로 실행하면") {
                then("필요한 빈들이 생성된다") {
                    contextRunner.run { context ->
                        context.getBean(GlobalExceptionHandler::class.java) shouldNotBe null
                        context.getBean(RequestLoggingFilter::class.java) shouldNotBe null
                    }
                }
            }
        }
    })
