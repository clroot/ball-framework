package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * InMemory Event Consumer 통합 테스트
 * Spring ApplicationEventPublisher를 통한 이벤트 발행과 Consumer 동작을 검증
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [IntegrationTestConfiguration::class])
@TestPropertySource(
    properties = [
        "ball.event.consumer.inmemory.enabled=true",
        "ball.event.consumer.inmemory.async=true"
    ]
)
class InMemoryEventBusIntegrationTest(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val integrationTestHandler: IntegrationTestEventHandler
) : BehaviorSpec({

    given("InMemory Event Consumer") {
        `when`("ApplicationEventPublisher로 DomainEventWrapper를 발행하는 경우") {
            then("Consumer가 이벤트를 수신하고 핸들러가 호출되어야 한다") {
                // Given
                val event = IntegrationTestEvent("test-data")
                val wrapper = DomainEventWrapper(event)

                // When
                applicationEventPublisher.publishEvent(wrapper)

                // Then
                await()
                    .atMost(Duration.ofSeconds(2))
                    .until { integrationTestHandler.callCount.get() > 0 }

                integrationTestHandler.callCount.get() shouldBe 1
                integrationTestHandler.lastEvent?.data shouldBe "test-data"
            }
        }

        `when`("여러 이벤트를 연속으로 발행하는 경우") {
            then("모든 이벤트가 처리되어야 한다") {
                // Given
                val events = (1..3).map { IntegrationTestEvent("data-$it") }
                val wrappers = events.map { DomainEventWrapper(it) }

                // When
                wrappers.forEach { applicationEventPublisher.publishEvent(it) }

                // Then
                await()
                    .atMost(Duration.ofSeconds(3))
                    .until { integrationTestHandler.callCount.get() >= 4 }  // 이전 테스트 1개 + 현재 3개

                integrationTestHandler.callCount.get() shouldBe 4
            }
        }
    }
})

/**
 * 통합 테스트용 도메인 이벤트
 */
data class IntegrationTestEvent(
    val data: String
) : DomainEvent {
    override val id: String = "integration-test-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "IntegrationTestEvent"
}

/**
 * 통합 테스트용 이벤트 핸들러
 */
@Component
class IntegrationTestEventHandler : DomainEventHandler<IntegrationTestEvent> {
    val callCount = AtomicInteger(0)
    var lastEvent: IntegrationTestEvent? = null

    override suspend fun handle(event: IntegrationTestEvent) {
        callCount.incrementAndGet()
        lastEvent = event
        // 실제 처리 시뮬레이션
        Thread.sleep(10)
    }
}

/**
 * 테스트용 DomainEventPublisher 구현체
 */
@Component
class TestDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {

    override fun publish(event: DomainEvent) {
        val wrapper = DomainEventWrapper(event)
        applicationEventPublisher.publishEvent(wrapper)
    }
}


/**
 * 테스트 설정
 */
@Configuration
@Import(
    DomainEventHandlerRegistry::class,
    InMemoryEventConsumerProperties::class,
    InMemoryEventListener::class,
)
class IntegrationTestConfiguration {

    @Bean
    fun integrationTestEventHandler(): IntegrationTestEventHandler = IntegrationTestEventHandler()

    @Bean
    fun testDomainEventPublisher(
        applicationEventPublisher: ApplicationEventPublisher
    ): DomainEventPublisher = TestDomainEventPublisher(applicationEventPublisher)
}
