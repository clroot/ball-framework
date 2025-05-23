package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.inbound.messaging.consumer.inmemory.registry.DomainEventHandlerRegistry
import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class InMemoryEventConsumerTest : BehaviorSpec({

    given("DomainEventHandlerRegistry") {
        `when`("DomainEventHandler들이 주입된 경우") {
            val handler1 = UnitTestEventHandler()
            val handler2 = AnotherUnitTestEventHandler()
            val registry = DomainEventHandlerRegistry(listOf(handler1, handler2))

            then("각 이벤트 타입별로 핸들러가 등록되어야 한다") {
                val handlers1 = registry.getHandlers(UnitTestDomainEvent::class.java)
                val handlers2 = registry.getHandlers(AnotherUnitTestDomainEvent::class.java)

                handlers1.size shouldBe 1
                handlers1[0] shouldBe handler1

                handlers2.size shouldBe 1
                handlers2[0] shouldBe handler2
            }
        }

        `when`("여러 핸들러가 같은 이벤트 타입을 처리하는 경우") {
            val handler1 = UnitTestEventHandler()
            val handler2 = UnitTestEventHandler()
            val registry = DomainEventHandlerRegistry(listOf(handler1, handler2))

            then("모든 핸들러가 등록되어야 한다") {
                val handlers = registry.getHandlers(UnitTestDomainEvent::class.java)
                handlers.size shouldBe 2
                handlers shouldContain handler1
                handlers shouldContain handler2
            }
        }

        `when`("등록되지 않은 이벤트 타입을 조회하는 경우") {
            val registry = DomainEventHandlerRegistry(emptyList())

            then("빈 리스트를 반환해야 한다") {
                val handlers = registry.getHandlers(UnitTestDomainEvent::class.java)
                handlers.size shouldBe 0
            }
        }

        `when`("등록된 이벤트 타입 목록을 조회하는 경우") {
            val handler1 = UnitTestEventHandler()
            val handler2 = AnotherUnitTestEventHandler()
            val registry = DomainEventHandlerRegistry(listOf(handler1, handler2))

            then("등록된 모든 이벤트 타입을 반환해야 한다") {
                val eventTypes = registry.getRegisteredEventTypes()
                eventTypes.size shouldBe 2
                eventTypes shouldContain UnitTestDomainEvent::class.java
                eventTypes shouldContain AnotherUnitTestDomainEvent::class.java
            }
        }
    }

    given("InMemoryEventConsumerProperties") {
        `when`("기본 설정을 사용하는 경우") {
            val properties = InMemoryEventConsumerProperties()

            then("기본값들이 올바르게 설정되어야 한다") {
                properties.enabled shouldBe true
                properties.async shouldBe true
                properties.parallel shouldBe true
                properties.maxConcurrency shouldBe 10
                properties.timeoutMs shouldBe 5000
                properties.enableRetry shouldBe false
                properties.maxRetryAttempts shouldBe 3
                properties.retryDelayMs shouldBe 1000
                properties.enableDebugLogging shouldBe false
            }
        }

        `when`("커스텀 설정을 사용하는 경우") {
            val properties = InMemoryEventConsumerProperties(
                enabled = false,
                async = false,
                parallel = false,
                maxConcurrency = 5,
                timeoutMs = 3000,
                enableRetry = true,
                maxRetryAttempts = 5,
                retryDelayMs = 500,
                enableDebugLogging = true
            )

            then("설정값들이 올바르게 반영되어야 한다") {
                properties.enabled shouldBe false
                properties.async shouldBe false
                properties.parallel shouldBe false
                properties.maxConcurrency shouldBe 5
                properties.timeoutMs shouldBe 3000
                properties.enableRetry shouldBe true
                properties.maxRetryAttempts shouldBe 5
                properties.retryDelayMs shouldBe 500
                properties.enableDebugLogging shouldBe true
            }
        }
    }

    given("DomainEventHandler 구현체") {
        `when`("이벤트를 처리하는 경우") {
            val handler = UnitTestEventHandler()
            val event = UnitTestDomainEvent()

            then("핸들러가 올바르게 동작해야 한다") {
                // When
                kotlinx.coroutines.runBlocking {
                    handler.handle(event)
                }

                // Then
                handler.handledEvents.size shouldBe 1
                handler.handledEvents[0] shouldBe event
                handler.callCount.get() shouldBe 1
            }
        }

        `when`("여러 이벤트를 연속으로 처리하는 경우") {
            val handler = UnitTestEventHandler()
            val events = (1..3).map { UnitTestDomainEvent() }

            then("모든 이벤트가 처리되어야 한다") {
                // When
                kotlinx.coroutines.runBlocking {
                    events.forEach { handler.handle(it) }
                }

                // Then
                handler.handledEvents.size shouldBe 3
                handler.callCount.get() shouldBe 3
                events.forEachIndexed { index, event ->
                    handler.handledEvents[index] shouldBe event
                }
            }
        }
    }
})

/**
 * 단위 테스트용 도메인 이벤트
 */
private class UnitTestDomainEvent : DomainEvent {
    override val id: String = "unit-test-id-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "UnitTestEvent"
}

private class AnotherUnitTestDomainEvent : DomainEvent {
    override val id: String = "another-unit-test-id-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "AnotherUnitTestEvent"
}

/**
 * 단위 테스트용 이벤트 핸들러
 */
private class UnitTestEventHandler : DomainEventHandler<UnitTestDomainEvent> {
    val handledEvents = mutableListOf<UnitTestDomainEvent>()
    val callCount = AtomicInteger(0)

    override suspend fun handle(event: UnitTestDomainEvent) {
        handledEvents.add(event)
        callCount.incrementAndGet()
        delay(10) // 약간의 처리 시간 시뮬레이션
    }
}

private class AnotherUnitTestEventHandler : DomainEventHandler<AnotherUnitTestDomainEvent> {
    val handledEvents = mutableListOf<AnotherUnitTestDomainEvent>()

    override suspend fun handle(event: AnotherUnitTestDomainEvent) {
        handledEvents.add(event)
        delay(10)
    }
}
