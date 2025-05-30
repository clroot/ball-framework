package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize

class DomainEventPublisherPropertiesTest : DescribeSpec({

    describe("DomainEventPublisherProperties") {
        
        context("기본값 설정") {
            it("기본 설정값들이 올바르게 설정되어야 한다") {
                // given & when
                val properties = DomainEventPublisherProperties()

                // then
                properties.enabled shouldBe true
                properties.async shouldBe true
                properties.enableRetry shouldBe true
                properties.maxRetryAttempts shouldBe 3
                properties.retryDelayMs shouldBe 1000
                properties.timeoutMs shouldBe 5000
                properties.enableDebugLogging shouldBe false
                properties.enableMetrics shouldBe true
            }

            it("기본 유효성 검증 설정이 올바르게 설정되어야 한다") {
                // given & when
                val properties = DomainEventPublisherProperties()
                val validation = properties.validation

                // then
                validation.strict shouldBe true
                validation.requiredFields shouldHaveSize 3
                validation.requiredFields shouldContain "id"
                validation.requiredFields shouldContain "type"
                validation.requiredFields shouldContain "occurredAt"
                validation.maxIdLength shouldBe 255
                validation.maxTypeLength shouldBe 100
            }
        }

        context("커스텀 설정") {
            it("모든 설정을 커스텀으로 변경할 수 있어야 한다") {
                // given & when
                val customValidation = DomainEventPublisherProperties.ValidationProperties(
                    strict = false,
                    requiredFields = listOf("id", "type"),
                    maxIdLength = 100,
                    maxTypeLength = 50
                )

                val properties = DomainEventPublisherProperties(
                    enabled = false,
                    async = false,
                    enableRetry = false,
                    maxRetryAttempts = 5,
                    retryDelayMs = 2000,
                    timeoutMs = 10000,
                    enableDebugLogging = true,
                    enableMetrics = false,
                    validation = customValidation
                )

                // then
                properties.enabled shouldBe false
                properties.async shouldBe false
                properties.enableRetry shouldBe false
                properties.maxRetryAttempts shouldBe 5
                properties.retryDelayMs shouldBe 2000
                properties.timeoutMs shouldBe 10000
                properties.enableDebugLogging shouldBe true
                properties.enableMetrics shouldBe false

                properties.validation.strict shouldBe false
                properties.validation.requiredFields shouldHaveSize 2
                properties.validation.maxIdLength shouldBe 100
                properties.validation.maxTypeLength shouldBe 50
            }
        }

        context("운영 환경 설정") {
            it("운영 환경에 적합한 설정을 만들 수 있어야 한다") {
                // given & when
                val productionProperties = DomainEventPublisherProperties(
                    enabled = true,
                    async = true,
                    enableRetry = true,
                    maxRetryAttempts = 3,
                    retryDelayMs = 1000,
                    timeoutMs = 5000,
                    enableDebugLogging = false, // 운영에서는 false
                    enableMetrics = true,       // 운영에서는 true
                    validation = DomainEventPublisherProperties.ValidationProperties(
                        strict = true,          // 운영에서는 엄격한 검증
                        requiredFields = listOf("id", "type", "occurredAt", "version"),
                        maxIdLength = 255,
                        maxTypeLength = 100
                    )
                )

                // then
                productionProperties.enableDebugLogging shouldBe false
                productionProperties.enableMetrics shouldBe true
                productionProperties.validation.strict shouldBe true
                productionProperties.validation.requiredFields shouldContain "version"
            }

            it("개발 환경에 적합한 설정을 만들 수 있어야 한다") {
                // given & when
                val developmentProperties = DomainEventPublisherProperties(
                    enabled = true,
                    async = false,              // 개발에서는 동기로 디버깅 편의
                    enableRetry = false,        // 개발에서는 빠른 실패
                    enableDebugLogging = true,  // 개발에서는 상세 로그
                    enableMetrics = false,      // 개발에서는 메트릭 수집 불필요
                    validation = DomainEventPublisherProperties.ValidationProperties(
                        strict = false,         // 개발에서는 느슨한 검증
                        requiredFields = listOf("id", "type"),
                        maxIdLength = 1000,     // 개발에서는 더 긴 ID 허용
                        maxTypeLength = 200
                    )
                )

                // then
                developmentProperties.async shouldBe false
                developmentProperties.enableRetry shouldBe false
                developmentProperties.enableDebugLogging shouldBe true
                developmentProperties.enableMetrics shouldBe false
                developmentProperties.validation.strict shouldBe false
            }
        }

        context("ValidationProperties") {
            it("빈 필수 필드 목록을 설정할 수 있어야 한다") {
                // given & when
                val validation = DomainEventPublisherProperties.ValidationProperties(
                    requiredFields = emptyList()
                )

                // then
                validation.requiredFields shouldHaveSize 0
            }

            it("여러 커스텀 필수 필드를 설정할 수 있어야 한다") {
                // given & when
                val customFields = listOf(
                    "id", "type", "occurredAt", 
                    "aggregateId", "version", "causationId", "correlationId"
                )
                val validation = DomainEventPublisherProperties.ValidationProperties(
                    requiredFields = customFields
                )

                // then
                validation.requiredFields shouldHaveSize 7
                validation.requiredFields shouldContain "aggregateId"
                validation.requiredFields shouldContain "version"
                validation.requiredFields shouldContain "causationId"
                validation.requiredFields shouldContain "correlationId"
            }
        }
    }
})
