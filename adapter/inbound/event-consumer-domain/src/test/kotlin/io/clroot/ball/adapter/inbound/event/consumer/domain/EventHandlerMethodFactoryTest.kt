package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethodFactory
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEvent
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEventHandler
import io.clroot.ball.application.port.inbound.ExecutorConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * ThreadPool 기반 EventHandlerMethodFactory 단위 테스트
 * 
 * 코루틴 기반에서 ThreadPool 기반으로 완전히 변경되었습니다.
 */
class EventHandlerMethodFactoryTest : FunSpec({

    test("should create ThreadPoolEventHandlerMethod from port") {
        val testHandler = TestDomainEventHandler.create()

        val handlerMethod = EventHandlerMethodFactory.createFromPort(testHandler)

        handlerMethod shouldNotBe null
        handlerMethod.port shouldBe testHandler
        handlerMethod.methodName shouldBe "TestDomainEventHandler.consume"
        handlerMethod.eventType shouldBe TestDomainEvent::class.java
        handlerMethod.order shouldBe testHandler.order
    }

    test("should create ThreadPoolEventHandlerMethod for test") {
        val testHandler = TestDomainEventHandler.create()

        val handlerMethod = EventHandlerMethodFactory.createForTest(
            testHandler, 
            TestDomainEvent::class.java
        )

        handlerMethod shouldNotBe null
        handlerMethod.port shouldBe testHandler
        handlerMethod.eventType shouldBe TestDomainEvent::class.java
    }

    test("should extract event type correctly") {
        val testHandler = TestDomainEventHandler.create()
        
        val eventType = EventHandlerMethodFactory.extractEventType(testHandler)
        
        eventType shouldBe TestDomainEvent::class.java
    }

    test("should handle ThreadPool configuration") {
        val testHandler = TestDomainEventHandler.create()
            .withExecutorConfig(
                ExecutorConfig(
                    corePoolSize = 2,
                    maxPoolSize = 5
                )
            )

        val handlerMethod = EventHandlerMethodFactory.createFromPort(testHandler)
        
        handlerMethod shouldNotBe null
        handlerMethod.port.executorConfig.corePoolSize shouldBe 2
        handlerMethod.port.executorConfig.maxPoolSize shouldBe 5
    }

    test("should execute event processing synchronously in test") {
        val testHandler = TestDomainEventHandler.create()
        val handlerMethod = EventHandlerMethodFactory.createFromPort(testHandler)
        
        val testEvent = TestDomainEvent.create(message = "Test message")
        
        // ThreadPool 기반에서는 invoke로 동기 실행
        handlerMethod.invoke(testEvent)
        
        testHandler.wasExecuted() shouldBe true
        testHandler.getExecutionCount() shouldBe 1
        testHandler.getLastExecutedEvent()?.message shouldBe "Test message"
    }

    test("should handle error scenarios") {
        val testHandler = TestDomainEventHandler.withException(RuntimeException("Test error"))
        val handlerMethod = EventHandlerMethodFactory.createFromPort(testHandler)
        
        val testEvent = TestDomainEvent.create()
        
        // 에러가 발생해도 팩토리는 정상적으로 생성되어야 함
        handlerMethod shouldNotBe null
        
        // 실행 시 에러 처리는 ErrorHandler에 위임됨
        try {
            handlerMethod.invoke(testEvent)
        } catch (e: Exception) {
            // 예상된 동작 - 에러 핸들러가 처리
        }
    }

    test("should support different executor configurations") {
        val conservativeHandler = TestDomainEventHandler.create()
            .withExecutorConfig(ExecutorConfig.conservative())
            
        val highThroughputHandler = TestDomainEventHandler.create()
            .withExecutorConfig(ExecutorConfig.highThroughput())

        val conservativeMethod = EventHandlerMethodFactory.createFromPort(conservativeHandler)
        val highThroughputMethod = EventHandlerMethodFactory.createFromPort(highThroughputHandler)

        conservativeMethod.port.executorConfig.maxPoolSize shouldBe 10
        highThroughputMethod.port.executorConfig.maxPoolSize shouldBe 100
    }
})
