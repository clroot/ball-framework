package io.clroot.ball.adapter.inbound.messaging.consumer.core.registry

import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class DomainEventHandlerRegistryTest : BehaviorSpec({

    given("DomainEventHandlerRegistry") {
        `when`("DomainEventHandler들이 주입된 경우") {
            val handler1 = TestEventHandler()
            val handler2 = AnotherTestEventHandler()
            val registry = DomainEventHandlerRegistry(listOf(handler1, handler2))

            then("각 이벤트 타입별로 핸들러가 등록되어야 한다") {
                val handlers1 = registry.getHandlers(TestDomainEvent::class.java)
                val handlers2 = registry.getHandlers(AnotherTestDomainEvent::class.java)

                handlers1.size shouldBe 1
                handlers1[0] shouldBe handler1

                handlers2.size shouldBe 1
                handlers2[0] shouldBe handler2
            }
        }

        `when`("여러 핸들러가 같은 이벤트 타입을 처리하는 경우") {
            val handler1 = TestEventHandler()
            val handler2 = TestEventHandler()
            val registry = DomainEventHandlerRegistry(listOf(handler1, handler2))

            then("모든 핸들러가 등록되어야 한다") {
                val handlers = registry.getHandlers(TestDomainEvent::class.java)
                handlers.size shouldBe 2
                handlers shouldContain handler1
                handlers shouldContain handler2
            }
        }

        `when`("등록되지 않은 이벤트 타입을 조회하는 경우") {
            val registry = DomainEventHandlerRegistry(emptyList())

            then("빈 리스트를 반환해야 한다") {
                val handlers = registry.getHandlers(TestDomainEvent::class.java)
                handlers.size shouldBe 0
            }
        }

        `when`("등록된 이벤트 타입 목록을 조회하는 경우") {
            val handler1 = TestEventHandler()
            val handler2 = AnotherTestEventHandler()
            val registry = DomainEventHandlerRegistry(listOf(handler1, handler2))

            then("등록된 모든 이벤트 타입을 반환해야 한다") {
                val eventTypes = registry.getRegisteredEventTypes()
                eventTypes.size shouldBe 2
                eventTypes shouldContain TestDomainEvent::class.java
                eventTypes shouldContain AnotherTestDomainEvent::class.java
            }
        }

        `when`("핸들러 개수를 조회하는 경우") {
            val handler1 = TestEventHandler()
            val handler2 = AnotherTestEventHandler()
            val handler3 = TestEventHandler() // 같은 이벤트 타입을 처리하는 또 다른 핸들러
            val registry = DomainEventHandlerRegistry(listOf(handler1, handler2, handler3))

            then("전체 및 이벤트 타입별 핸들러 개수를 정확히 반환해야 한다") {
                registry.getHandlerCount() shouldBe 3
                registry.getHandlerCount(TestDomainEvent::class.java) shouldBe 2
                registry.getHandlerCount(AnotherTestDomainEvent::class.java) shouldBe 1
            }
        }
    }
})

/**
 * 테스트용 도메인 이벤트
 */
private data class TestDomainEvent(
    val data: String = "test-data"
) : DomainEvent {
    override val id: String = "test-id-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "TestDomainEvent"
}

private data class AnotherTestDomainEvent(
    val value: Int = 42
) : DomainEvent {
    override val id: String = "another-test-id-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "AnotherTestDomainEvent"
}

/**
 * 테스트용 이벤트 핸들러
 */
private class TestEventHandler : DomainEventHandler<TestDomainEvent> {
    val handledEvents = mutableListOf<TestDomainEvent>()
    val callCount = AtomicInteger(0)

    override suspend fun handle(event: TestDomainEvent) {
        handledEvents.add(event)
        callCount.incrementAndGet()
        delay(10) // 약간의 처리 시간 시뮬레이션
    }
}

private class AnotherTestEventHandler : DomainEventHandler<AnotherTestDomainEvent> {
    val handledEvents = mutableListOf<AnotherTestDomainEvent>()
    val callCount = AtomicInteger(0)

    override suspend fun handle(event: AnotherTestDomainEvent) {
        handledEvents.add(event)
        callCount.incrementAndGet()
        delay(10)
    }
}
