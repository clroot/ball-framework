package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventHandlerMethodFactory
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEvent
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEventHandler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * EventHandlerMethodFactory 단위 테스트
 * 
 * 팩토리의 기본 기능을 검증합니다.
 */
class EventHandlerMethodFactoryTest : FunSpec({

    test("should create EventHandlerMethod from port") {
        val testHandler = TestDomainEventHandler()

        val handlerMethod = EventHandlerMethodFactory.createFromPort(testHandler)

        handlerMethod shouldNotBe null
        handlerMethod.bean shouldBe testHandler
        handlerMethod.methodName shouldBe "TestDomainEventHandler.consume"
        handlerMethod.eventType shouldBe TestDomainEvent::class.java
        handlerMethod.async shouldBe testHandler.async
        handlerMethod.order shouldBe testHandler.order
    }

    test("should validate handler method correctly") {
        val testHandler = TestDomainEventHandler()
        val handlerMethod = EventHandlerMethodFactory.createFromPort(testHandler)

        val isValid = EventHandlerMethodFactory.validateHandlerMethod(
            handlerMethod.method, 
            TestDomainEvent::class.java
        )

        isValid shouldBe true
    }

    test("should extract event type correctly") {
        val testHandler = TestDomainEventHandler()
        
        val eventType = EventHandlerMethodFactory.extractEventType(testHandler)
        
        eventType shouldBe TestDomainEvent::class.java
    }
})
