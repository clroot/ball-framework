package io.clroot.ball.adapter.inbound.rest.integration

import io.clroot.ball.adapter.inbound.rest.config.RestAdapterAutoConfiguration
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

/**
 * 기본 통합 테스트
 */
class RestAdapterIntegrationTest : BehaviorSpec({

    val contextRunner = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                WebMvcAutoConfiguration::class.java,
                RestAdapterAutoConfiguration::class.java
            )
        )

    given("기본 통합 환경에서") {
        `when`("auto-configuration이 실행되면") {
            then("정상적으로 구성된다") {
                contextRunner.run { context ->
                    context.startupFailure shouldBe null
                }
            }
        }
    }
})
