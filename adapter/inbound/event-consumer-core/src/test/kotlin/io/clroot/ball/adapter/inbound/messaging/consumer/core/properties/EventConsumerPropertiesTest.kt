package io.clroot.ball.adapter.inbound.messaging.consumer.core.properties

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EventConsumerPropertiesTest : BehaviorSpec({

    given("EventConsumerProperties") {
        `when`("기본 설정을 사용하는 경우") {
            val properties = EventConsumerProperties()

            then("기본값들이 올바르게 설정되어야 한다") {
                properties.enabled shouldBe true
                properties.async shouldBe true
                properties.parallel shouldBe true
                properties.maxConcurrency shouldBe 10
                properties.timeoutMs shouldBe 30000
                properties.enableRetry shouldBe true
                properties.maxRetryAttempts shouldBe 3
                properties.retryDelayMs shouldBe 1000
                
                // 에러 핸들링 기본값 확인
                properties.errorHandling.enableDeadLetterQueue shouldBe false
                properties.errorHandling.logLevel shouldBe "ERROR"
                properties.errorHandling.enableNotification shouldBe false
            }
        }

        `when`("커스텀 설정을 사용하는 경우") {
            val customErrorHandling = ErrorHandlingProperties(
                enableDeadLetterQueue = true,
                logLevel = "WARN",
                enableNotification = true
            )
            
            val properties = EventConsumerProperties(
                enabled = false,
                async = false,
                parallel = false,
                maxConcurrency = 5,
                timeoutMs = 15000,
                enableRetry = false,
                maxRetryAttempts = 5,
                retryDelayMs = 500,
                errorHandling = customErrorHandling
            )

            then("설정값들이 올바르게 반영되어야 한다") {
                properties.enabled shouldBe false
                properties.async shouldBe false
                properties.parallel shouldBe false
                properties.maxConcurrency shouldBe 5
                properties.timeoutMs shouldBe 15000
                properties.enableRetry shouldBe false
                properties.maxRetryAttempts shouldBe 5
                properties.retryDelayMs shouldBe 500
                
                // 커스텀 에러 핸들링 확인
                properties.errorHandling.enableDeadLetterQueue shouldBe true
                properties.errorHandling.logLevel shouldBe "WARN"
                properties.errorHandling.enableNotification shouldBe true
            }
        }

        `when`("부분적으로 커스텀 설정을 사용하는 경우") {
            val properties = EventConsumerProperties(
                maxConcurrency = 20,
                timeoutMs = 60000,
                enableRetry = false
            )

            then("지정된 값은 변경되고 나머지는 기본값이어야 한다") {
                // 변경된 값들
                properties.maxConcurrency shouldBe 20
                properties.timeoutMs shouldBe 60000
                properties.enableRetry shouldBe false
                
                // 기본값들
                properties.enabled shouldBe true
                properties.async shouldBe true
                properties.parallel shouldBe true
                properties.maxRetryAttempts shouldBe 3
                properties.retryDelayMs shouldBe 1000
            }
        }
    }

    given("ErrorHandlingProperties") {
        `when`("기본 설정을 사용하는 경우") {
            val errorHandling = ErrorHandlingProperties()

            then("기본값들이 올바르게 설정되어야 한다") {
                errorHandling.enableDeadLetterQueue shouldBe false
                errorHandling.logLevel shouldBe "ERROR"
                errorHandling.enableNotification shouldBe false
            }
        }

        `when`("커스텀 설정을 사용하는 경우") {
            val errorHandling = ErrorHandlingProperties(
                enableDeadLetterQueue = true,
                logLevel = "DEBUG",
                enableNotification = true
            )

            then("설정값들이 올바르게 반영되어야 한다") {
                errorHandling.enableDeadLetterQueue shouldBe true
                errorHandling.logLevel shouldBe "DEBUG"
                errorHandling.enableNotification shouldBe true
            }
        }

        `when`("다양한 로그 레벨 설정을 사용하는 경우") {
            val errorLevels = listOf("ERROR", "WARN", "INFO", "DEBUG")

            then("모든 로그 레벨이 올바르게 설정되어야 한다") {
                errorLevels.forEach { level ->
                    val errorHandling = ErrorHandlingProperties(logLevel = level)
                    errorHandling.logLevel shouldBe level
                }
            }
        }
    }
})
