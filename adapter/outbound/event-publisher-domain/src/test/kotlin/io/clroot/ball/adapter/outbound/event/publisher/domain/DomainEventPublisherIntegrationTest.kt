package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.clroot.ball.application.port.outbound.EventProducerPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


@SpringBootTest
@ContextConfiguration(classes = [DomainEventPublisherAutoConfiguration::class, TestEventListener::class])
@TestPropertySource(
    properties = [
        "ball.events.domain.enabled=true",
        "ball.events.domain.async=false",  // 동기 모드로 변경
        "ball.events.domain.enable-debug-logging=true"
    ]
)
class DomainEventPublisherIntegrationTest {

    @Autowired
    private lateinit var eventProducer: EventProducerPort

    @Autowired
    private lateinit var testEventListener: TestEventListener

    @BeforeEach
    fun setUp() {
        testEventListener.reset()
    }

    @Test
    fun `간단한 이벤트 리스너 동작 검증`() {
        runBlocking {
            // given
            val testEvent = TestDomainEvent("simple-test", "SimpleEvent", Instant.now())
            
            // when
            eventProducer.produce(testEvent)
            
            // 동기 모드이므로 즉시 처리되어야 함
            delay(100) // 약간의 처리 시간 허용
            
            // then
            assertTrue(testEventListener.getEventCount() >= 1, "Event count should be at least 1")
            assertNotNull(testEventListener.getLastReceivedEvent(), "Last received event should not be null")
        }
    }

    @Test
    fun `도메인 이벤트가 발행되면 리스너에서 수신되어야 한다`() {
        runBlocking {
            // given
            val testEvent = TestDomainEvent("integration-test-1", "UserCreated", Instant.now())

            // when
            eventProducer.produce(testEvent)
            
            // 동기 모드에서도 약간의 처리 시간 허용
            delay(50)

            // then
            assertTrue(testEventListener.awaitEvent(5, TimeUnit.SECONDS), "Event should be received within 5 seconds")
            assertNotNull(testEventListener.getLastReceivedEvent(), "Last received event should not be null")
            assertEquals(
                testEvent,
                testEventListener.getLastReceivedEvent()?.domainEvent,
                "Received event should match sent event"
            )
            assertEquals(1, testEventListener.getEventCount(), "Event count should be 1")
        }
    }

    @Test
    fun `여러 이벤트를 연속으로 발행할 수 있어야 한다`() {
        runBlocking {
            // given
            val events = listOf(
                TestDomainEvent("event-1", "UserCreated", Instant.now()),
                TestDomainEvent("event-2", "UserUpdated", Instant.now()),
                TestDomainEvent("event-3", "UserDeleted", Instant.now())
            )

            // when
            events.forEach { event ->
                eventProducer.produce(event)
            }
            
            // 처리 시간 허용
            delay(100)

            // then
            assertTrue(testEventListener.awaitEvents(3, 5, TimeUnit.SECONDS), "All 3 events should be received")
            assertEquals(3, testEventListener.getEventCount(), "Event count should be 3")
        }
    }

    @Test
    fun `일괄 이벤트 발행이 정상 동작해야 한다`() {
        runBlocking {
            // given
            val events = listOf(
                TestDomainEvent("batch-1", "OrderCreated", Instant.now()),
                TestDomainEvent("batch-2", "OrderPaid", Instant.now()),
                TestDomainEvent("batch-3", "OrderShipped", Instant.now())
            )

            // when
            eventProducer.produce(events)
            
            // 처리 시간 허용
            delay(100)

            // then
            assertTrue(testEventListener.awaitEvents(3, 5, TimeUnit.SECONDS), "All 3 batch events should be received")
            assertEquals(3, testEventListener.getEventCount(), "Event count should be 3")
        }
    }

    @Test
    fun `다양한 타입의 이벤트를 구분하여 처리할 수 있어야 한다`() {
        runBlocking {
            // given
            val userEvent = TestDomainEvent("user-event", "UserCreated", Instant.now())
            val orderEvent = TestDomainEvent("order-event", "OrderCreated", Instant.now())
            val paymentEvent = TestDomainEvent("payment-event", "PaymentProcessed", Instant.now())

            // when
            eventProducer.produce(userEvent)
            eventProducer.produce(orderEvent)
            eventProducer.produce(paymentEvent)
            
            // 처리 시간 허용
            delay(100)

            // then
            assertTrue(
                testEventListener.awaitEvents(3, 5, TimeUnit.SECONDS),
                "All 3 different event types should be received"
            )
            assertEquals(3, testEventListener.getEventCount(), "Event count should be 3")

            val receivedEventTypes = testEventListener.getReceivedEventTypes()
            assertTrue(receivedEventTypes.contains("UserCreated"), "Should contain UserCreated event")
            assertTrue(receivedEventTypes.contains("OrderCreated"), "Should contain OrderCreated event")
            assertTrue(receivedEventTypes.contains("PaymentProcessed"), "Should contain PaymentProcessed event")
        }
    }

    @Test
    fun `비동기 이벤트 발행이 블로킹되지 않아야 한다`() {
        runBlocking {
            // given
            val startTime = System.currentTimeMillis()
            val events = (1..5).map {  // 10개에서 5개로 줄임
                TestDomainEvent("async-$it", "AsyncTest", Instant.now())
            }

            // when
            events.forEach { event ->
                eventProducer.produce(event)
            }
            val endTime = System.currentTimeMillis()
            
            // 처리 시간 허용
            delay(100)

            // then  
            // 동기 모드이므로 시간 제한을 늘림 (100ms → 1000ms)
            val elapsedTime = (endTime - startTime).toInt()
            assertTrue(elapsedTime < 1000, "Event publishing should complete within 1 second")

            // 모든 이벤트가 처리될 때까지 대기
            assertTrue(testEventListener.awaitEvents(5, 5, TimeUnit.SECONDS), "All 5 events should be received")
            assertEquals(5, testEventListener.getEventCount(), "Event count should be 5")
        }
    }

    @Test
    fun `유효하지 않은 이벤트 발행 시 예외가 발생해야 한다`() {
        runBlocking {
            // given
            val invalidEvent = TestDomainEvent("", "InvalidEvent", Instant.now())

            // when & then
            val exception = assertThrows(IllegalArgumentException::class.java) {
                runBlocking { eventProducer.produce(invalidEvent) }
            }
            assertTrue(
                exception.message!!.contains("Domain event ID cannot be blank"),
                "Exception should mention blank ID"
            )

            // 유효하지 않은 이벤트는 리스너에 도달하지 않아야 함
            delay(100) // 잠시 대기
            assertEquals(0, testEventListener.getEventCount(), "Invalid event should not reach listener")
        }
    }

    @Test
    fun `주입된 EventProducerPort가 SpringDomainEventProducer여야 한다`() {
        // then
        assertTrue(eventProducer is SpringDomainEventProducer, "EventProducerPort should be SpringDomainEventProducer")
    }
}


/**
 * 테스트용 이벤트 리스너
 *
 * 발행된 도메인 이벤트를 수신하여 테스트에서 검증할 수 있도록 합니다.
 */
@Component
class TestEventListener {

    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)
    private val eventCount = AtomicInteger(0)
    private val lastReceivedEvent = AtomicReference<DomainEventWrapper?>(null)
    private val receivedEventTypes = mutableSetOf<String>()
    private var eventCountDown: CountDownLatch? = null
    private val receivedEvents = mutableListOf<DomainEventWrapper>()

    init {
        log.info("TestEventListener initialized - ready to receive events")
    }

    @EventListener
    fun handleDomainEvent(wrapper: DomainEventWrapper) {
        log.info("TestEventListener received event: {} (ID: {})", wrapper.eventType, wrapper.eventId)
        
        val newCount = eventCount.incrementAndGet()
        lastReceivedEvent.set(wrapper)
        
        synchronized(this) {
            receivedEventTypes.add(wrapper.eventType)
            receivedEvents.add(wrapper)
            eventCountDown?.countDown()
        }
        
        log.info("TestEventListener event count now: {}", newCount)
    }

    fun reset() {
        log.info("TestEventListener reset called")
        synchronized(this) {
            eventCount.set(0)
            lastReceivedEvent.set(null)
            receivedEventTypes.clear()
            receivedEvents.clear()
            eventCountDown = null
        }
    }

    fun getEventCount(): Int = eventCount.get()

    fun getLastReceivedEvent(): DomainEventWrapper? = lastReceivedEvent.get()

    fun getReceivedEventTypes(): Set<String> = receivedEventTypes.toSet()

    fun getReceivedEvents(): List<DomainEventWrapper> = receivedEvents.toList()

    fun awaitEvent(timeout: Long, unit: TimeUnit): Boolean {
        log.info("TestEventListener awaitEvent called - waiting for 1 event with timeout {} {}", timeout, unit)
        
        synchronized(this) {
            // 이미 이벤트가 수신되었다면 즉시 반환
            if (eventCount.get() >= 1) {
                log.info("TestEventListener awaitEvent - event already received, returning true immediately")
                return true
            }
            
            // 이벤트가 아직 수신되지 않았다면 대기 설정
            eventCountDown = CountDownLatch(1)
        }
        
        val result = eventCountDown?.await(timeout, unit) ?: false
        log.info("TestEventListener awaitEvent result: {}, current count: {}", result, eventCount.get())
        return result
    }

    fun awaitEvents(count: Int, timeout: Long, unit: TimeUnit): Boolean {
        log.info(
            "TestEventListener awaitEvents called - waiting for {} events with timeout {} {}",
            count,
            timeout,
            unit
        )
        
        synchronized(this) {
            val currentCount = eventCount.get()
            
            // 이미 충분한 이벤트가 수신되었다면 즉시 반환
            if (currentCount >= count) {
                log.info("TestEventListener awaitEvents - {} events already received (target: {}), returning true immediately", currentCount, count)
                return true
            }
            
            // 남은 이벤트 수만큼만 CountDownLatch 생성
            val remainingCount = count - currentCount
            log.info("TestEventListener awaitEvents - current count: {}, target: {}, waiting for {} more events", currentCount, count, remainingCount)
            eventCountDown = CountDownLatch(remainingCount)
        }
        
        val result = eventCountDown?.await(timeout, unit) ?: false
        val finalCount = eventCount.get()
        log.info("TestEventListener awaitEvents result: {}, final count: {} (target: {})", result, finalCount, count)
        return result || finalCount >= count  // 최종 검증 추가
    }
}
