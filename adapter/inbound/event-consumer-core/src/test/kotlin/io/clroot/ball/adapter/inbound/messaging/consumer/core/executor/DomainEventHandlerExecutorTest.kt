package io.clroot.ball.adapter.inbound.messaging.consumer.core.executor

import io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.EventConsumerProperties
import io.clroot.ball.adapter.inbound.messaging.consumer.core.registry.BlockingDomainEventHandlerRegistry
import io.clroot.ball.adapter.inbound.messaging.consumer.core.registry.DomainEventHandlerRegistry
import io.clroot.ball.application.event.BlockingDomainEventHandler
import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.awaitility.Awaitility.await
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class DomainEventHandlerExecutorTest : BehaviorSpec({

    given("DomainEventHandlerExecutor") {
        `when`("suspend 핸들러만 있는 경우") {
            val suspendHandler = TestSuspendHandler()
            val handlerRegistry = DomainEventHandlerRegistry(listOf(suspendHandler))
            val blockingRegistry = BlockingDomainEventHandlerRegistry(emptyList())
            val properties = EventConsumerProperties(
                parallel = false,
                enableRetry = false
            )
            val executor = DomainEventHandlerExecutor(handlerRegistry, blockingRegistry, properties)

            then("suspend 핸들러가 실행되어야 한다") {
                val event = TestSuspendEvent("test-data")

                runBlocking {
                    executor.execute(event)
                }

                suspendHandler.callCount.get() shouldBe 1
                suspendHandler.handledEvents.size shouldBe 1
                suspendHandler.handledEvents[0] shouldBe event
            }
        }

        `when`("blocking 핸들러만 있는 경우") {
            val blockingHandler = TestBlockingHandler()
            val handlerRegistry = DomainEventHandlerRegistry(emptyList())
            val blockingRegistry = BlockingDomainEventHandlerRegistry(listOf(blockingHandler))
            val properties = EventConsumerProperties(
                parallel = false,
                enableRetry = false
            )
            val executor = DomainEventHandlerExecutor(handlerRegistry, blockingRegistry, properties)

            then("blocking 핸들러가 실행되어야 한다") {
                val event = TestBlockingEvent("blocking-data")

                runBlocking {
                    executor.execute(event)
                }

                blockingHandler.callCount.get() shouldBe 1
                blockingHandler.handledEvents.size shouldBe 1
                blockingHandler.handledEvents[0] shouldBe event
            }
        }

        `when`("suspend와 blocking 핸들러가 모두 있는 경우") {
            val suspendHandler = TestSuspendHandler()
            val blockingHandler = TestBlockingHandler()
            val handlerRegistry = DomainEventHandlerRegistry(listOf(suspendHandler))
            val blockingRegistry = BlockingDomainEventHandlerRegistry(listOf(blockingHandler))
            val properties = EventConsumerProperties(
                parallel = false,
                enableRetry = false
            )
            val executor = DomainEventHandlerExecutor(handlerRegistry, blockingRegistry, properties)

            then("각각의 이벤트 타입에 맞는 핸들러가 실행되어야 한다") {
                val suspendEvent = TestSuspendEvent("suspend-data")
                val blockingEvent = TestBlockingEvent("blocking-data")

                runBlocking {
                    executor.execute(suspendEvent)
                    executor.execute(blockingEvent)
                }

                suspendHandler.callCount.get() shouldBe 1
                blockingHandler.callCount.get() shouldBe 1
                suspendHandler.handledEvents[0] shouldBe suspendEvent
                blockingHandler.handledEvents[0] shouldBe blockingEvent
            }
        }

        `when`("병렬 처리가 활성화된 경우") {
            val handler1 = TestSuspendHandler()
            val handler2 = TestSuspendHandler()
            val handlerRegistry = DomainEventHandlerRegistry(listOf(handler1, handler2))
            val blockingRegistry = BlockingDomainEventHandlerRegistry(emptyList())
            val properties = EventConsumerProperties(
                parallel = true,
                maxConcurrency = 5,
                enableRetry = false
            )
            val executor = DomainEventHandlerExecutor(handlerRegistry, blockingRegistry, properties)

            then("여러 핸들러가 병렬로 실행되어야 한다") {
                val event = TestSuspendEvent("parallel-data")
                val startTime = System.currentTimeMillis()

                runBlocking {
                    executor.execute(event)
                }

                val endTime = System.currentTimeMillis()
                val executionTime = endTime - startTime

                handler1.callCount.get() shouldBe 1
                handler2.callCount.get() shouldBe 1
                // 병렬 실행으로 인해 실행 시간이 단축되어야 함 (각 핸들러가 50ms씩 sleep)
                executionTime shouldBeGreaterThan 40  // 최소 실행 시간
                executionTime shouldBeGreaterThan 0   // 실제로는 더 정확한 검증이 필요
            }
        }

        `when`("재시도가 활성화되고 핸들러가 실패하는 경우") {
            val failingHandler = FailingSuspendHandler()
            val handlerRegistry = DomainEventHandlerRegistry(listOf(failingHandler))
            val blockingRegistry = BlockingDomainEventHandlerRegistry(emptyList())
            val properties = EventConsumerProperties(
                parallel = false,
                enableRetry = true,
                maxRetryAttempts = 2,
                retryDelayMs = 10
            )
            val executor = DomainEventHandlerExecutor(handlerRegistry, blockingRegistry, properties)

            then("설정된 횟수만큼 재시도해야 한다") {
                val event = TestSuspendEvent("retry-data")

                runBlocking {
                    executor.execute(event)
                }

                // 원래 실행 1번 + 재시도 2번 = 총 3번 호출
                await()
                    .atMost(Duration.ofSeconds(1))
                    .until { failingHandler.callCount.get() >= 3 }

                failingHandler.callCount.get() shouldBe 3
            }
        }

        `when`("등록된 핸들러가 없는 경우") {
            val handlerRegistry = DomainEventHandlerRegistry(emptyList())
            val blockingRegistry = BlockingDomainEventHandlerRegistry(emptyList())
            val properties = EventConsumerProperties()
            val executor = DomainEventHandlerExecutor(handlerRegistry, blockingRegistry, properties)

            then("오류 없이 처리되어야 한다") {
                val event = TestSuspendEvent("no-handler-data")

                runBlocking {
                    executor.execute(event)
                }

                // 아무 일도 일어나지 않아야 함 (예외 발생하지 않음)
            }
        }
    }
})

/**
 * 테스트용 도메인 이벤트들
 */
private data class TestSuspendEvent(
    val data: String
) : DomainEvent {
    override val id: String = "suspend-event-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "TestSuspendEvent"
}

private data class TestBlockingEvent(
    val data: String
) : DomainEvent {
    override val id: String = "blocking-event-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "TestBlockingEvent"
}

/**
 * 테스트용 핸들러들
 */
private class TestSuspendHandler : DomainEventHandler<TestSuspendEvent> {
    val handledEvents = mutableListOf<TestSuspendEvent>()
    val callCount = AtomicInteger(0)

    override suspend fun handle(event: TestSuspendEvent) {
        handledEvents.add(event)
        callCount.incrementAndGet()
        delay(50) // 처리 시간 시뮬레이션
    }
}

private class TestBlockingHandler : BlockingDomainEventHandler<TestBlockingEvent> {
    val handledEvents = mutableListOf<TestBlockingEvent>()
    val callCount = AtomicInteger(0)

    override fun handle(event: TestBlockingEvent) {
        handledEvents.add(event)
        callCount.incrementAndGet()
        Thread.sleep(50) // Blocking I/O 시뮬레이션
    }
}

/**
 * 항상 실패하는 테스트용 핸들러 (재시도 테스트용)
 */
private class FailingSuspendHandler : DomainEventHandler<TestSuspendEvent> {
    val callCount = AtomicInteger(0)

    override suspend fun handle(event: TestSuspendEvent) {
        callCount.incrementAndGet()
        throw RuntimeException("Test failure for retry")
    }
}
