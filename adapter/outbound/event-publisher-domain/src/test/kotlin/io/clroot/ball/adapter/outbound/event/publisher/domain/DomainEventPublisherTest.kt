package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class DomainEventPublisherTest : BehaviorSpec({

    given("DomainEventPublisher") {
        val applicationEventPublisher = mockk<ApplicationEventPublisher>()
        val properties = DomainEventPublisherProperties(
            async = true,
            enableDebugLogging = true,
            enableRetry = true
        )
        val publisher = SpringDomainEventProducer(applicationEventPublisher, properties)

        `when`("valid domain event is published") {
            val testEvent = TestDomainEvent("test-id", "TestEvent", Instant.now())

            every { applicationEventPublisher.publishEvent(any<DomainEventWrapper>()) } just Runs

            then("event should be published successfully") {
                publisher.produce(testEvent)

                verify(exactly = 1) {
                    applicationEventPublisher.publishEvent(any<DomainEventWrapper>())
                }
            }
        }

        `when`("event with blank ID is published") {
            val invalidEvent = TestDomainEvent("", "TestEvent", Instant.now())

            then("should throw IllegalArgumentException") {
                val exception = runCatching { publisher.produce(invalidEvent) }
                exception.isFailure shouldBe true
                exception.exceptionOrNull() shouldNotBe null
            }
        }

        `when`("event with blank type is published") {
            val invalidEvent = TestDomainEvent("test-id", "", Instant.now())

            then("should throw IllegalArgumentException") {
                val exception = runCatching { publisher.produce(invalidEvent) }
                exception.isFailure shouldBe true
                exception.exceptionOrNull() shouldNotBe null
            }
        }

        `when`("ApplicationEventPublisher throws exception") {
            val testEvent = TestDomainEvent("test-id", "TestEvent", Instant.now())
            val publishException = RuntimeException("Publisher failed")

            every { applicationEventPublisher.publishEvent(any<DomainEventWrapper>()) } throws publishException

            then("should propagate the exception") {
                val exception = runCatching { publisher.produce(testEvent) }
                exception.isFailure shouldBe true
                exception.exceptionOrNull() shouldBe publishException
            }
        }
    }

    given("DomainEventPublisher with retry disabled") {
        val applicationEventPublisher = mockk<ApplicationEventPublisher>()
        val properties = DomainEventPublisherProperties(enableRetry = false)
        val publisher = SpringDomainEventProducer(applicationEventPublisher, properties)

        `when`("publishing fails") {
            val testEvent = TestDomainEvent("test-id", "TestEvent", Instant.now())
            val publishException = RuntimeException("Publisher failed")

            every { applicationEventPublisher.publishEvent(any<DomainEventWrapper>()) } throws publishException

            then("should not attempt retry") {
                val exception = runCatching { publisher.produce(testEvent) }
                exception.isFailure shouldBe true

                // 재시도가 비활성화되어 있으므로 1번만 호출되어야 함
                verify(exactly = 1) {
                    applicationEventPublisher.publishEvent(any<DomainEventWrapper>())
                }
            }
        }
    }
})

/**
 * 테스트용 도메인 이벤트
 */
data class TestDomainEvent(
    override val id: String,
    override val type: String,
    override val occurredAt: Instant
) : DomainEvent
