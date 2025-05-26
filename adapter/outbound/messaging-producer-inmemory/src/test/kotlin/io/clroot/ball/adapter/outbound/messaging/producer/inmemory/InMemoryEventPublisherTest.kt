package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.*
import kotlinx.coroutines.delay
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class InMemoryEventPublisherTest : BehaviorSpec({

    given("InMemoryEventPublisher") {
        val applicationEventPublisher = mockk<ApplicationEventPublisher>()

        `when`("동기 모드로 설정된 경우") {
            val properties = InMemoryEventPublisherProperties(async = false)
            val publisher = InMemoryEventPublisher(
                applicationEventPublisher, properties
            )

            then("이벤트를 동기적으로 처리해야 한다") {
                // Given
                val event = TestDomainEvent()
                every { applicationEventPublisher.publishEvent(any<DomainEventWrapper>()) } just Runs

                // When
                publisher.publish(event)

                // Then
                verify(exactly = 1) {
                    applicationEventPublisher.publishEvent(match<DomainEventWrapper> {
                        it.domainEvent == event
                    })
                }
            }
        }

        `when`("비동기 모드로 설정된 경우") {
            val properties = InMemoryEventPublisherProperties(async = true)
            val publisher = InMemoryEventPublisher(
                applicationEventPublisher, properties
            )

            then("이벤트를 비동기적으로 처리해야 한다") {
                // Given
                val event = TestDomainEvent()
                every { applicationEventPublisher.publishEvent(any<DomainEventWrapper>()) } just Runs

                // When
                publisher.publish(event)

                // Then (비동기이므로 잠시 대기)
                delay(100)
                verify(exactly = 1) {
                    applicationEventPublisher.publishEvent(match<DomainEventWrapper> {
                        it.domainEvent == event
                    })
                }
            }
        }
    }
})

/**
 * 테스트용 도메인 이벤트
 */
private class TestDomainEvent : DomainEvent {
    override val id: String = "test-id"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "TestEvent"
}
