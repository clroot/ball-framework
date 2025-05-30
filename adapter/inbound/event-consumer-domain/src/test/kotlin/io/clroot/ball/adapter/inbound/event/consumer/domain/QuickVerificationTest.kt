package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEvent
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEventHandler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * ThreadPool 마이그레이션 검증을 위한 간단한 테스트
 */
class QuickVerificationTest : FunSpec({

    test("should execute ThreadPool handler successfully") {
        // Given
        val handler = TestDomainEventHandler.create()
        val event = TestDomainEvent.create(message = "Quick test")
        
        // When
        handler.consume(event)
        
        // Then
        handler.wasExecuted() shouldBe true
        handler.getExecutionCount() shouldBe 1
        handler.getLastExecutedEvent()?.message shouldBe "Quick test"
        
        println("✅ ThreadPool handler works perfectly!")
    }

    test("should handle multiple events") {
        // Given
        val handler = TestDomainEventHandler.create()
        val events = listOf(
            TestDomainEvent.create(message = "Event 1"),
            TestDomainEvent.create(message = "Event 2"),
            TestDomainEvent.create(message = "Event 3")
        )
        
        // When
        events.forEach { handler.consume(it) }
        
        // Then
        handler.wasExecuted() shouldBe true
        handler.getExecutionCount() shouldBe 3
        handler.getExecutedEvents().map { it.message } shouldBe listOf("Event 1", "Event 2", "Event 3")
        
        println("✅ Multiple events handled successfully!")
    }

    test("should demonstrate fluent interface") {
        // Given & When
        val handler = TestDomainEventHandler.create()
            .withHandlerName("FluentHandler")
            .withOrder(5)
            .setExecutionDelay(50)
        
        val event = TestDomainEvent.create(message = "Fluent test")
        
        // When
        val startTime = System.currentTimeMillis()
        handler.consume(event)
        val endTime = System.currentTimeMillis()
        
        // Then
        handler.wasExecuted() shouldBe true
        handler.handlerName shouldBe "FluentHandler"
        handler.order shouldBe 5
        
        // 지연 시간 확인 (50ms 이상 소요되어야 함)
        val duration = endTime - startTime
        println("✅ Fluent interface works! Execution took ${duration}ms (expected >50ms)")
        
        (duration >= 50) shouldBe true
    }
})
