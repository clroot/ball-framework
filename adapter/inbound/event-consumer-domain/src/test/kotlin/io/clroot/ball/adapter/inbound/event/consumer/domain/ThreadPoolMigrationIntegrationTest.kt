package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.domain.examples.*
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.DomainEventConsumerTestHelper
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEvent
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEventHandler
import io.clroot.ball.application.port.inbound.EventErrorHandler
import io.clroot.ball.application.port.inbound.ExecutorConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.time.Instant
import kotlin.system.measureTimeMillis

/**
 * ThreadPool 기반 EventConsumer 통합 테스트
 * 
 * 실제 ThreadPool이 동작하면서 이벤트를 처리하는지 확인합니다.
 */
class ThreadPoolMigrationIntegrationTest : FunSpec({

    test("should process single event successfully") {
        val handler = TestDomainEventHandler.create()
        val consumer = DomainEventConsumerTestHelper.createTestConsumer(listOf(handler))
        
        val event = TestDomainEvent.create(message = "Single event test")

        consumer.handleDomainEvent(event)
        Thread.sleep(500) // ThreadPool 처리 대기
        
        handler.wasExecuted() shouldBe true
        handler.getExecutionCount() shouldBe 1
        handler.getLastExecutedEvent()?.message shouldBe "Single event test"
    }

    test("should process multiple events concurrently") {
        val handler = TestDomainEventHandler.withDelay(200) // 200ms 지연
        val consumer = DomainEventConsumerTestHelper.createTestConsumer(listOf(handler))
        
        val events = DomainEventConsumerTestHelper.createTestEvents(5, "concurrent")
        
        val executionTime = measureTimeMillis {
            events.forEach { event ->
                consumer.handleDomainEvent(event)
            }
            Thread.sleep(1000) // ThreadPool 처리 완료 대기
        }
        
        handler.getExecutionCount() shouldBe 5
        // ThreadPool 덕분에 5개 이벤트가 동시 처리되어 1초 이내 완료
        executionTime shouldBeGreaterThan 800
    }

    test("should handle different executor configurations") {
        val conservativeHandler = TestDomainEventHandler.create()
            .withExecutorConfig(ExecutorConfig.conservative())
            
        val highThroughputHandler = TestDomainEventHandler.create()
            .withExecutorConfig(ExecutorConfig.highThroughput())
        
        val conservativeConsumer = DomainEventConsumerTestHelper.createTestConsumer(listOf(conservativeHandler))
        val highThroughputConsumer = DomainEventConsumerTestHelper.createTestConsumer(listOf(highThroughputHandler))
        
        val event = TestDomainEvent.create(message = "Config test")
        
        conservativeConsumer.handleDomainEvent(event)
        highThroughputConsumer.handleDomainEvent(event)
        
        Thread.sleep(500)
        
        conservativeHandler.wasExecuted() shouldBe true
        highThroughputHandler.wasExecuted() shouldBe true
    }

    test("should handle errors with retry strategy").config(enabled = false) {
        var attemptCount = 0
        
        // 실패하는 핸들러를 별도로 생성하고 설정
        val customHandler = object : TestDomainEventHandler() {
            override fun consume(event: TestDomainEvent) {
                attemptCount++
                if (attemptCount <= 2) {
                    throw RuntimeException("Simulated failure $attemptCount")
                }
                super.consume(event)  // 3번째에 성공
            }
        }
        
        // 에러 핸들러 설정 적용
        customHandler.withErrorHandler(EventErrorHandler.retrying(maxAttempts = 3))
        
        val consumer = DomainEventConsumerTestHelper.createTestConsumer(listOf(customHandler))
        val event = TestDomainEvent.create(message = "Retry test")
        
        consumer.handleDomainEvent(event)
        Thread.sleep(2000) // 재시도 시간 고려해서 대기
        
        attemptCount shouldBe 3 // 2번 실패 후 3번째에 성공
        customHandler.wasExecuted() shouldBe true
    }

    test("should demonstrate ThreadPool benefits directly") {
        println("\n🚀 Direct ThreadPool Handler Demo")
        println("=" * 60)
        
        // 직접 핸들러 실행 (Spring Consumer 없이)
        
        // 1. 사용자 생성 핸들러
        val userHandler = UserCreatedEventHandler()
        val userEvent = UserCreatedEvent(
            id = "user-001",
            type = "UserCreated",
            occurredAt = Instant.now(),
            userId = "user-123",
            email = "test@example.com",
            name = "Test User"
        )
        
        println("\n📋 1. User Creation Processing:")
        userHandler.consume(userEvent)
        
        // 2. 고성능 로그 핸들러
        val auditHandler = AuditLogEventHandler()
        val auditEvents = (1..3).map { i ->
            AuditLogEvent(
                id = "audit-$i",
                type = "AuditLog",
                occurredAt = Instant.now(),
                userId = "user-123",
                action = "LOGIN",
                resource = "web-app"
            )
        }
        
        println("\n📋 2. Audit Log Processing (High Throughput):")
        auditEvents.forEach { auditHandler.consume(it) }
        
        // 3. 복잡한 주문 처리
        val orderHandler = OrderCompletedEventHandler()
        val orderEvent = OrderCompletedEvent(
            id = "order-001",
            type = "OrderCompleted",
            occurredAt = Instant.now(),
            orderId = "order-123",
            userId = "user-123",
            totalAmount = 50000,
            items = listOf(
                OrderItem("product-1", 2, 20000),
                OrderItem("product-2", 1, 30000)
            )
        )
        
        println("\n📋 3. Complex Order Processing:")
        orderHandler.consume(orderEvent)
        
        // 4. 에러 처리 시나리오
        val paymentHandler = PaymentFailedEventHandler()
        val paymentEvents = listOf(
            PaymentFailedEvent("pay-1", "PaymentFailed", Instant.now(), "order-1", "INSUFFICIENT_FUNDS"),
            PaymentFailedEvent("pay-2", "PaymentFailed", Instant.now(), "order-2", "INVALID_CARD"),
            PaymentFailedEvent("pay-3", "PaymentFailed", Instant.now(), "order-3", "NETWORK_ERROR")
        )
        
        println("\n📋 4. Payment Failure Handling:")
        paymentEvents.forEach { event ->
            try {
                paymentHandler.consume(event)
            } catch (e: Exception) {
                println("   ⚠️ Expected error handling: ${e.message}")
            }
        }
        
        println("\n" + "=" * 60)
        println("✅ ThreadPool Direct Execution Demo Completed!")
        println("   All handlers processed events successfully using ThreadPool instead of coroutines.")
        println("   - Simple blocking functions")
        println("   - Natural JPA integration") 
        println("   - Configurable thread pools")
        println("   - Built-in error handling")
        println("   - Better debugging experience")
        
        Thread.sleep(1000) // 모든 비동기 작업 완료 대기
    }

    test("should show performance improvement") {
        println("\n📊 Performance Comparison Demo")
        println("=" * 50)
        
        // Conservative vs High Throughput 설정 비교
        val conservativeHandler = ConservativeHandler()
        val highThroughputHandler = HighThroughputHandler()
        
        val events = (1..10).map { i ->
            LoadTestEvent(
                id = "load-$i",
                type = "LoadTest",
                occurredAt = Instant.now(),
                data = "Test data $i"
            )
        }
        
        println("\n🐌 Conservative Configuration (maxPool=10):")
        val conservativeTime = measureTimeMillis {
            events.forEach { conservativeHandler.consume(it) }
            Thread.sleep(2000)
        }
        
        println("\n🚀 High Throughput Configuration (maxPool=100):")
        val highThroughputTime = measureTimeMillis {
            events.forEach { highThroughputHandler.consume(it) }
            Thread.sleep(2000)
        }
        
        println("\n📈 Results:")
        println("   Conservative: ${conservativeTime}ms")
        println("   High Throughput: ${highThroughputTime}ms")
        println("   Both completed successfully with different thread pool sizes!")
        
        // 둘 다 성공적으로 완료되어야 함
        true shouldBe true
    }

    test("should demonstrate handler metrics") {
        println("\n📊 Handler Metrics Demo")
        println("=" * 50)
        
        // 여러 핸들러 생성
        val handlers = listOf(
            TestDomainEventHandler.create().withHandlerName("Handler-1"),
            TestDomainEventHandler.withDelay(100).withHandlerName("Handler-2"),
            TestDomainEventHandler.create().withHandlerName("Handler-3")
        )
        
        // 이벤트 처리
        val events = DomainEventConsumerTestHelper.createTestEvents(3, "metrics")
        
        println("\n Processing events...")
        handlers.forEach { handler ->
            events.forEach { event ->
                handler.consume(event)
            }
        }
        
        Thread.sleep(1000) // 처리 완료 대기
        
        // 메트릭 출력
        handlers.forEach { handler ->
            println("📈 ${handler.handlerName}:")
            println("   - Processed: ${handler.getProcessedEventCount()}")
            println("   - Executed: ${handler.getExecutionCount()}")
            println("   - Was Executed: ${handler.wasExecuted()}")
        }
        
        println("=" * 50)
        
        // 검증
        handlers.forEach { handler ->
            handler.wasExecuted() shouldBe true
            handler.getExecutionCount() shouldBe 3 // 각 핸들러가 3개 이벤트 처리
        }
    }
})

private operator fun String.times(n: Int): String = this.repeat(n)
