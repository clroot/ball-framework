package io.clroot.ball.adapter.inbound.messaging.consumer.core.registry

import io.clroot.ball.application.event.BlockingDomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class BlockingDomainEventHandlerRegistryTest : BehaviorSpec({

    given("BlockingDomainEventHandlerRegistry") {
        `when`("BlockingDomainEventHandler들이 주입된 경우") {
            val handler1 = TestBlockingEventHandler()
            val handler2 = AnotherTestBlockingEventHandler()
            val registry = BlockingDomainEventHandlerRegistry(listOf(handler1, handler2))

            then("각 이벤트 타입별로 핸들러가 등록되어야 한다") {
                val handlers1 = registry.getHandlers(TestBlockingDomainEvent::class.java)
                val handlers2 = registry.getHandlers(AnotherTestBlockingDomainEvent::class.java)

                handlers1.size shouldBe 1
                handlers1[0] shouldBe handler1

                handlers2.size shouldBe 1
                handlers2[0] shouldBe handler2
            }
        }

        `when`("여러 핸들러가 같은 이벤트 타입을 처리하는 경우") {
            val handler1 = TestBlockingEventHandler()
            val handler2 = TestBlockingEventHandler()
            val registry = BlockingDomainEventHandlerRegistry(listOf(handler1, handler2))

            then("모든 핸들러가 등록되어야 한다") {
                val handlers = registry.getHandlers(TestBlockingDomainEvent::class.java)
                handlers.size shouldBe 2
                handlers shouldContain handler1
                handlers shouldContain handler2
            }
        }

        `when`("등록되지 않은 이벤트 타입을 조회하는 경우") {
            val registry = BlockingDomainEventHandlerRegistry(emptyList())

            then("빈 리스트를 반환해야 한다") {
                val handlers = registry.getHandlers(TestBlockingDomainEvent::class.java)
                handlers.size shouldBe 0
            }
        }

        `when`("등록된 이벤트 타입 목록을 조회하는 경우") {
            val handler1 = TestBlockingEventHandler()
            val handler2 = AnotherTestBlockingEventHandler()
            val registry = BlockingDomainEventHandlerRegistry(listOf(handler1, handler2))

            then("등록된 모든 이벤트 타입을 반환해야 한다") {
                val eventTypes = registry.getRegisteredEventTypes()
                eventTypes.size shouldBe 2
                eventTypes shouldContain TestBlockingDomainEvent::class.java
                eventTypes shouldContain AnotherTestBlockingDomainEvent::class.java
            }
        }

        `when`("핸들러 개수를 조회하는 경우") {
            val handler1 = TestBlockingEventHandler()
            val handler2 = AnotherTestBlockingEventHandler()
            val handler3 = TestBlockingEventHandler() // 같은 이벤트 타입을 처리하는 또 다른 핸들러
            val registry = BlockingDomainEventHandlerRegistry(listOf(handler1, handler2, handler3))

            then("전체 및 이벤트 타입별 핸들러 개수를 정확히 반환해야 한다") {
                registry.getHandlerCount() shouldBe 3
                registry.getHandlerCount(TestBlockingDomainEvent::class.java) shouldBe 2
                registry.getHandlerCount(AnotherTestBlockingDomainEvent::class.java) shouldBe 1
            }
        }
    }
})

/**
 * 테스트용 블로킹 도메인 이벤트
 */
private data class TestBlockingDomainEvent(
    val data: String = "blocking-test-data"
) : DomainEvent {
    override val id: String = "blocking-test-id-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "TestBlockingDomainEvent"
}

private data class AnotherTestBlockingDomainEvent(
    val value: Int = 123
) : DomainEvent {
    override val id: String = "another-blocking-test-id-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "AnotherTestBlockingDomainEvent"
}

/**
 * 테스트용 블로킹 이벤트 핸들러
 */
private class TestBlockingEventHandler : BlockingDomainEventHandler<TestBlockingDomainEvent> {
    val handledEvents = mutableListOf<TestBlockingDomainEvent>()
    val callCount = AtomicInteger(0)

    override fun handle(event: TestBlockingDomainEvent) {
        handledEvents.add(event)
        callCount.incrementAndGet()
        Thread.sleep(10) // Blocking I/O 시뮬레이션
    }
}

private class AnotherTestBlockingEventHandler : BlockingDomainEventHandler<AnotherTestBlockingDomainEvent> {
    val handledEvents = mutableListOf<AnotherTestBlockingDomainEvent>()
    val callCount = AtomicInteger(0)

    override fun handle(event: AnotherTestBlockingDomainEvent) {
        handledEvents.add(event)
        callCount.incrementAndGet()
        Thread.sleep(10)
    }
}
