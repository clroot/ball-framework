package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.domain.test.DomainEventConsumerTestHelper
import io.clroot.ball.adapter.inbound.event.consumer.domain.test.TestDomainEventHandler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.shouldNotBe

/**
 * 간단한 도메인 이벤트 컨슈머 테스트
 *
 * 복잡한 기능 검증 전에 기본 동작을 확인합니다.
 */
class SimpleDomainEventConsumerTest : FunSpec({

    test("should create test consumer without errors") {
        val testHandler = TestDomainEventHandler()

        val consumer = DomainEventConsumerTestHelper.createTestConsumer(
            handlers = listOf(testHandler),
            properties = DomainEventConsumerTestHelper.createTestProperties(async = false)
        )

        // 컨슈머가 정상적으로 생성되어야 함
        consumer shouldNotBe null
    }

    test("should process single event with single handler") {
        val testHandler = TestDomainEventHandler()

        val consumer = DomainEventConsumerTestHelper.createTestConsumer(
            handlers = listOf(testHandler),
            properties = DomainEventConsumerTestHelper.createTestProperties(async = false)
        )

        val testEvent = DomainEventConsumerTestHelper.createTestEvent(
            id = "simple-test-event",
            type = "SimpleTestEvent"
        )

        // 이벤트 처리
        consumer.handleDomainEvent(testEvent)

        // 핸들러가 실행되었는지 확인
        testHandler.wasExecuted() shouldBe true
        testHandler.getExecutionCount() shouldBe 1
    }

    test("should handle no handlers gracefully") {
        val consumer = DomainEventConsumerTestHelper.createTestConsumer(
            handlers = emptyList(), // 핸들러 없음
            properties = DomainEventConsumerTestHelper.createTestProperties(async = false)
        )

        val testEvent = DomainEventConsumerTestHelper.createTestEvent(
            id = "no-handler-event",
            type = "NoHandlerEvent"
        )

        // 예외 없이 처리되어야 함
        consumer.handleDomainEvent(testEvent)

        // 정상 완료되면 성공
        testEvent.id shouldBe "no-handler-event"
    }
})
