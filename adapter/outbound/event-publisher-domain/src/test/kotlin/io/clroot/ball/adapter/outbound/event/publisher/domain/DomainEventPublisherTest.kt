package io.clroot.ball.adapter.outbound.event.publisher.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class DomainEventPublisherTest : DescribeSpec({

    describe("SpringDomainEventProducer") {

        context("기본 이벤트 발행") {
            it("유효한 도메인 이벤트가 정상적으로 발행되어야 한다") {
                // given
                val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                val properties = DomainEventPublisherProperties()
                val producer = SpringDomainEventProducer(mockPublisher, properties)
                val testEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())

                // when
                producer.produce(testEvent)

                // then
                verify(exactly = 1) {
                    mockPublisher.publishEvent(any())
                }
            }

            it("여러 이벤트를 일괄 발행할 수 있어야 한다") {
                runBlocking {
                    // given
                    val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                    val properties = DomainEventPublisherProperties()
                    val producer = SpringDomainEventProducer(mockPublisher, properties)

                    val events = listOf(
                        TestDomainEvent("event-1", "UserCreated", Instant.now()),
                        TestDomainEvent("event-2", "UserUpdated", Instant.now())
                    )

                    // when
                    producer.produce(events)

                    // then
                    verify(exactly = 2) {
                        mockPublisher.publishEvent(any())
                    }
                }
            }
        }

        context("유효성 검증") {
            it("ID가 공백인 이벤트 발행 시 예외가 발생해야 한다") {
                // given
                val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                val properties = DomainEventPublisherProperties()
                val producer = SpringDomainEventProducer(mockPublisher, properties)
                val invalidEvent = TestDomainEvent("", "UserCreated", Instant.now())

                // when & then
                val exception = shouldThrow<IllegalArgumentException> {
                    producer.produce(invalidEvent)
                }
                exception.message shouldContain "Domain event ID cannot be blank"

                // 유효성 검증에서 실패했으므로 publishEvent는 호출되지 않아야 함
                verify(exactly = 0) {
                    mockPublisher.publishEvent(any())
                }
            }

            it("Type이 공백인 이벤트 발행 시 예외가 발생해야 한다") {
                // given
                val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                val properties = DomainEventPublisherProperties()
                val producer = SpringDomainEventProducer(mockPublisher, properties)
                val invalidEvent = TestDomainEvent("test-id", "", Instant.now())

                // when & then
                val exception = shouldThrow<IllegalArgumentException> {
                    producer.produce(invalidEvent)
                }
                exception.message shouldContain "Domain event type cannot be blank"

                verify(exactly = 0) {
                    mockPublisher.publishEvent(any())
                }
            }
        }

        context("에러 처리") {
            it("ApplicationEventPublisher에서 예외 발생 시 예외가 전파되어야 한다") {
                // given
                val mockPublisher = mockk<ApplicationEventPublisher>()
                val properties = DomainEventPublisherProperties(enableRetry = false)
                val producer = SpringDomainEventProducer(mockPublisher, properties)
                val testEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())
                val publishException = RuntimeException("Publisher failed")

                every { mockPublisher.publishEvent(any()) } throws publishException

                // when & then
                val exception = shouldThrow<RuntimeException> {
                    producer.produce(testEvent)
                }
                exception shouldBe publishException

                verify(exactly = 1) {
                    mockPublisher.publishEvent(any())
                }
            }

            it("재시도가 비활성화된 경우 한 번만 시도해야 한다") {
                // given
                val mockPublisher = mockk<ApplicationEventPublisher>()
                val properties = DomainEventPublisherProperties(enableRetry = false)
                val producer = SpringDomainEventProducer(mockPublisher, properties)
                val testEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())

                every { mockPublisher.publishEvent(any()) } throws RuntimeException("Failed")

                // when
                shouldThrow<RuntimeException> {
                    producer.produce(testEvent)
                }

                // then
                verify(exactly = 1) {
                    mockPublisher.publishEvent(any())
                }
            }
        }

        context("설정별 동작") {
            it("디버그 로깅이 활성화되어도 정상 동작해야 한다") {
                // given
                val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                val properties = DomainEventPublisherProperties(
                    enableDebugLogging = true,
                    enableRetry = false
                )
                val producer = SpringDomainEventProducer(mockPublisher, properties)
                val testEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())

                // when
                producer.produce(testEvent)

                // then
                verify(exactly = 1) {
                    mockPublisher.publishEvent(any())
                }
            }

            it("비동기/동기 모드 모두 정상 동작해야 한다") {
                // given
                val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                val asyncProperties = DomainEventPublisherProperties(async = true)
                val syncProperties = DomainEventPublisherProperties(async = false)
                val asyncProducer = SpringDomainEventProducer(mockPublisher, asyncProperties)
                val syncProducer = SpringDomainEventProducer(mockPublisher, syncProperties)
                val testEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())

                // when
                asyncProducer.produce(testEvent)
                syncProducer.produce(testEvent)

                // then
                verify(exactly = 2) {
                    mockPublisher.publishEvent(any())
                }
            }
        }

        context("EventProducerPort 인터페이스 호환성") {
            it("Event 인터페이스를 통해 DomainEvent를 발행할 수 있어야 한다") {
                runBlocking {
                    // given
                    val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                    val properties = DomainEventPublisherProperties()
                    val producer = SpringDomainEventProducer(mockPublisher, properties)
                    val testEvent = TestDomainEvent("test-id", "UserCreated", Instant.now())

                    // when
                    producer.produce(testEvent as io.clroot.ball.domain.event.Event)

                    // then
                    verify(exactly = 1) {
                        mockPublisher.publishEvent(any())
                    }
                }
            }

            it("지원하지 않는 Event 타입 발행 시 예외가 발생해야 한다") {
                runBlocking {
                    // given
                    val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                    val properties = DomainEventPublisherProperties()
                    val producer = SpringDomainEventProducer(mockPublisher, properties)
                    val unsupportedEvent = UnsupportedTestEvent("test-id", "UnsupportedEvent", Instant.now())

                    // when & then
                    val exception = shouldThrow<IllegalArgumentException> {
                        producer.produce(unsupportedEvent)
                    }
                    exception.message shouldContain "Unsupported event type"

                    verify(exactly = 0) {
                        mockPublisher.publishEvent(any())
                    }
                }
            }
        }

        context("빈 리스트 처리") {
            it("빈 이벤트 리스트를 처리할 수 있어야 한다") {
                runBlocking {
                    // given
                    val mockPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
                    val properties = DomainEventPublisherProperties()
                    val producer = SpringDomainEventProducer(mockPublisher, properties)

                    // when
                    producer.produce(emptyList())

                    // then
                    verify(exactly = 0) {
                        mockPublisher.publishEvent(any())
                    }
                }
            }
        }
    }
})
