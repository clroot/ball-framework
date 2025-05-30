package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.clroot.ball.adapter.shared.messaging.DomainEventWrapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.springframework.context.ApplicationEvent
import java.time.Instant

class DomainEventWrapperTest : DescribeSpec({

    describe("DomainEventWrapper") {
        
        context("기본 기능") {
            it("도메인 이벤트를 래핑할 수 있어야 한다") {
                // given
                val domainEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())

                // when
                val wrapper = DomainEventWrapper(domainEvent)

                // then
                wrapper.domainEvent shouldBe domainEvent
                wrapper.eventType shouldBe "UserCreated"
                wrapper.eventId shouldBe "test-id"
            }

            it("ApplicationEvent를 상속해야 한다") {
                // given
                val domainEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())

                // when
                val wrapper = DomainEventWrapper(domainEvent)

                // then
                (wrapper is ApplicationEvent) shouldBe true
                wrapper.source shouldBe domainEvent
            }
        }

        context("프로퍼티 접근") {
            it("eventType 프로퍼티로 이벤트 타입에 접근할 수 있어야 한다") {
                // given
                val domainEvent = TestDomainEvent("test-id", "OrderCompleted", Instant.now())
                val wrapper = DomainEventWrapper(domainEvent)

                // when & then
                wrapper.eventType shouldBe "OrderCompleted"
                wrapper.eventType shouldBe domainEvent.type
            }

            it("eventId 프로퍼티로 이벤트 ID에 접근할 수 있어야 한다") {
                // given
                val domainEvent = TestDomainEvent("order-123", "OrderCompleted", Instant.now())
                val wrapper = DomainEventWrapper(domainEvent)

                // when & then
                wrapper.eventId shouldBe "order-123"
                wrapper.eventId shouldBe domainEvent.id
            }

            it("원본 domainEvent에 직접 접근할 수 있어야 한다") {
                // given
                val domainEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())
                val wrapper = DomainEventWrapper(domainEvent)

                // when & then
                wrapper.domainEvent shouldBe domainEvent
                wrapper.domainEvent.id shouldBe "test-id"
                wrapper.domainEvent.type shouldBe "UserCreated"
                wrapper.domainEvent.occurredAt shouldNotBe null
            }
        }

        context("toString 메서드") {
            it("의미 있는 문자열 표현을 제공해야 한다") {
                // given
                val domainEvent = TestDomainEvent("user-456", "UserUpdated", Instant.now())
                val wrapper = DomainEventWrapper(domainEvent)

                // when
                val stringRepresentation = wrapper.toString()

                // then
                stringRepresentation shouldContain "DomainEventWrapper"
                stringRepresentation shouldContain "eventType='UserUpdated'"
                stringRepresentation shouldContain "eventId='user-456'"
            }
        }

        context("다양한 도메인 이벤트 타입") {
            it("복잡한 도메인 이벤트도 래핑할 수 있어야 한다") {
                // given
                val complexEvent = ComplexDomainEvent(
                    id = "complex-event-123",
                    type = "OrderProcessingCompleted",
                    occurredAt = Instant.now(),
                    orderId = "order-789",
                    customerId = "customer-456",
                    totalAmount = 12500,
                    items = listOf("item1", "item2", "item3")
                )

                // when
                val wrapper = DomainEventWrapper(complexEvent)

                // then
                wrapper.domainEvent shouldBe complexEvent
                wrapper.eventType shouldBe "OrderProcessingCompleted"
                wrapper.eventId shouldBe "complex-event-123"
                
                // 원본 이벤트의 복잡한 데이터에도 접근 가능
                (wrapper.domainEvent as ComplexDomainEvent).orderId shouldBe "order-789"
                (wrapper.domainEvent as ComplexDomainEvent).totalAmount shouldBe 12500
                (wrapper.domainEvent as ComplexDomainEvent).items shouldBe listOf("item1", "item2", "item3")
            }

            it("빈 값을 가진 이벤트도 래핑할 수 있어야 한다") {
                // given
                val emptyEvent = TestDomainEvent("", "", Instant.now())

                // when
                val wrapper = DomainEventWrapper(emptyEvent)

                // then
                wrapper.eventType shouldBe ""
                wrapper.eventId shouldBe ""
                wrapper.domainEvent shouldBe emptyEvent
            }
        }

        context("Spring 이벤트 시스템 통합") {
            it("Spring ApplicationEvent로서 동작해야 한다") {
                // given
                val domainEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())
                val wrapper = DomainEventWrapper(domainEvent)

                // when & then
                // ApplicationEvent의 기본 기능들이 정상 작동해야 함
                wrapper.timestamp shouldNotBe 0L
                wrapper.source shouldBe domainEvent
            }

            it("동일한 도메인 이벤트를 래핑한 래퍼들의 동등성을 확인할 수 있어야 한다") {
                // given
                val domainEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())
                val wrapper1 = DomainEventWrapper(domainEvent)
                val wrapper2 = DomainEventWrapper(domainEvent)

                // when & then
                // 동일한 도메인 이벤트를 래핑했으므로 동등해야 함
                wrapper1.domainEvent shouldBe wrapper2.domainEvent
                wrapper1.eventType shouldBe wrapper2.eventType
                wrapper1.eventId shouldBe wrapper2.eventId
            }
        }
    }
})


