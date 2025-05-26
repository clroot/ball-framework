package io.clroot.ball.adapter.inbound.messaging.consumer.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.clroot.ball.application.event.BlockingDomainEventHandler
import io.clroot.ball.application.event.DomainEventHandler
import io.clroot.ball.domain.event.DomainEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kafka Event Consumer 통합 테스트
 *
 * EmbeddedKafka를 사용하여 실제 Kafka 환경과 유사한 조건에서 테스트합니다.
 */
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [KafkaIntegrationTestConfiguration::class])
@EmbeddedKafka(
    partitions = 1,
    topics = ["integration-test-events"],
    brokerProperties = [
        "auto.create.topics.enable=true"
    ]
)
@TestPropertySource(
    properties = [
        "ball.event.consumer.kafka.enabled=true",
        "ball.event.consumer.kafka.topics=integration-test-events",
        "ball.event.consumer.kafka.groupId=integration-test-group",
        "ball.event.consumer.kafka.bootstrapServers=\${spring.embedded.kafka.brokers}",
        "ball.event.consumer.kafka.autoOffsetReset=earliest",
        "ball.event.consumer.kafka.async=false", // 테스트에서는 동기 처리
        "ball.event.consumer.kafka.enableRetry=false",
        "ball.event.consumer.kafka.concurrency=1",
        "ball.event.consumer.kafka.enableAutoCommit=false"
    ]
)
class KafkaEventConsumerIntegrationTest(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val embeddedKafkaBroker: EmbeddedKafkaBroker,
    private val suspendTestHandler: IntegrationSuspendTestHandler,
    private val blockingTestHandler: IntegrationBlockingTestHandler
) : BehaviorSpec({

    val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    given("Kafka Event Consumer Integration") {
        `when`("올바른 형식의 suspend 이벤트 메시지를 Kafka로 전송하는 경우") {
            then("Consumer가 메시지를 수신하고 suspend 핸들러가 실행되어야 한다") {
                // Given
                val event = IntegrationSuspendTestEvent("suspend-integration-data")
                val kafkaMessage = createKafkaMessage(event, objectMapper)

                // When
                kafkaTemplate.send("integration-test-events", kafkaMessage).get()

                // Then
                await()
                    .atMost(Duration.ofSeconds(30))
                    .until { suspendTestHandler.callCount.get() > 0 }

                suspendTestHandler.callCount.get() shouldBe 1
                suspendTestHandler.lastEvent?.data shouldBe "suspend-integration-data"
            }
        }

        `when`("올바른 형식의 blocking 이벤트 메시지를 Kafka로 전송하는 경우") {
            then("Consumer가 메시지를 수신하고 blocking 핸들러가 실행되어야 한다") {
                // Given
                val event = IntegrationBlockingTestEvent("blocking-integration-data")
                val kafkaMessage = createKafkaMessage(event, objectMapper)

                // When
                kafkaTemplate.send("integration-test-events", kafkaMessage).get()

                // Then
                await()
                    .atMost(Duration.ofSeconds(10))
                    .until { blockingTestHandler.callCount.get() > 0 }

                blockingTestHandler.callCount.get() shouldBe 1
                blockingTestHandler.lastEvent?.data shouldBe "blocking-integration-data"
            }
        }

        `when`("여러 이벤트를 연속으로 전송하는 경우") {
            then("모든 이벤트가 순서대로 처리되어야 한다") {
                // Given
                val events = (1..3).map { IntegrationSuspendTestEvent("batch-data-$it") }
                val kafkaMessages = events.map { createKafkaMessage(it, objectMapper) }

                val initialCount = suspendTestHandler.callCount.get()

                // When
                kafkaMessages.forEach { message ->
                    kafkaTemplate.send("integration-test-events", message).get()
                }

                // Then
                await()
                    .atMost(Duration.ofSeconds(15))
                    .until { suspendTestHandler.callCount.get() >= initialCount + 3 }

                val finalCount = suspendTestHandler.callCount.get()
                (finalCount - initialCount) shouldBe 3
            }
        }

        `when`("잘못된 형식의 메시지를 전송하는 경우") {
            then("메시지가 스킵되고 에러가 발생하지 않아야 한다") {
                // Given
                val invalidMessage = """{"invalid": "message", "format": true}"""
                val initialSuspendCount = suspendTestHandler.callCount.get()
                val initialBlockingCount = blockingTestHandler.callCount.get()

                // When
                kafkaTemplate.send("integration-test-events", invalidMessage).get()

                // Then - 잘못된 메시지는 핸들러가 호출되지 않아야 함
                Thread.sleep(2000) // 잠시 대기

                suspendTestHandler.callCount.get() shouldBe initialSuspendCount
                blockingTestHandler.callCount.get() shouldBe initialBlockingCount
            }
        }

        `when`("존재하지 않는 이벤트 타입의 메시지를 전송하는 경우") {
            then("메시지 변환 실패로 처리되고 핸들러가 호출되지 않아야 한다") {
                // Given
                val messageWithInvalidEventType = """
                    {
                        "eventType": "com.nonexistent.Event",
                        "eventData": {
                            "id": "invalid-event-123",
                            "data": "should-not-be-processed"
                        }
                    }
                """.trimIndent()

                val initialSuspendCount = suspendTestHandler.callCount.get()
                val initialBlockingCount = blockingTestHandler.callCount.get()

                // When
                kafkaTemplate.send("integration-test-events", messageWithInvalidEventType).get()

                // Then
                Thread.sleep(2000) // 잠시 대기

                suspendTestHandler.callCount.get() shouldBe initialSuspendCount
                blockingTestHandler.callCount.get() shouldBe initialBlockingCount
            }
        }

        `when`("대량의 메시지를 전송하는 경우") {
            then("모든 메시지가 처리되어야 한다") {
                // Given
                val messageCount = 10
                val events = (1..messageCount).map { IntegrationSuspendTestEvent("bulk-data-$it") }
                val kafkaMessages = events.map { createKafkaMessage(it, objectMapper) }

                val initialCount = suspendTestHandler.callCount.get()

                // When
                kafkaMessages.forEach { message ->
                    kafkaTemplate.send("integration-test-events", message)
                }
                kafkaTemplate.flush() // 모든 메시지 전송 완료 대기

                // Then
                await()
                    .atMost(Duration.ofSeconds(30))
                    .until { suspendTestHandler.callCount.get() >= initialCount + messageCount }

                val finalCount = suspendTestHandler.callCount.get()
                (finalCount - initialCount) shouldBeGreaterThan 8 // 최소 80% 이상 처리
            }
        }
    }
})

/**
 * Kafka 메시지 생성 헬퍼 함수
 */
private fun createKafkaMessage(event: DomainEvent, objectMapper: ObjectMapper): String {
    val messageData = mapOf(
        "eventType" to event.javaClass.name,
        "eventId" to event.id,
        "occurredAt" to event.occurredAt.toString(),
        "eventData" to event
    )
    return objectMapper.writeValueAsString(messageData)
}

/**
 * 통합 테스트용 도메인 이벤트들
 */
data class IntegrationSuspendTestEvent(
    val data: String
) : DomainEvent {
    override val id: String = "integration-suspend-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "IntegrationSuspendTestEvent"
}

data class IntegrationBlockingTestEvent(
    val data: String
) : DomainEvent {
    override val id: String = "integration-blocking-${System.nanoTime()}"
    override val occurredAt: Instant = Instant.now()
    override val type: String = "IntegrationBlockingTestEvent"
}

/**
 * 통합 테스트용 이벤트 핸들러들
 */
@Component
class IntegrationSuspendTestHandler : DomainEventHandler<IntegrationSuspendTestEvent> {
    val callCount = AtomicInteger(0)
    var lastEvent: IntegrationSuspendTestEvent? = null

    override suspend fun handle(event: IntegrationSuspendTestEvent) {
        callCount.incrementAndGet()
        lastEvent = event
        // 처리 시뮬레이션
        kotlinx.coroutines.delay(50)
    }
}

@Component
class IntegrationBlockingTestHandler : BlockingDomainEventHandler<IntegrationBlockingTestEvent> {
    val callCount = AtomicInteger(0)
    var lastEvent: IntegrationBlockingTestEvent? = null

    override fun handle(event: IntegrationBlockingTestEvent) {
        callCount.incrementAndGet()
        lastEvent = event
        // Blocking 처리 시뮬레이션
        Thread.sleep(50)
    }
}

/**
 * 통합 테스트 설정
 */
@Configuration
@EnableKafka
@Import(KafkaEventConsumerAutoConfiguration::class)
class KafkaIntegrationTestConfiguration {

    @Bean
    fun integrationSuspendTestHandler(): IntegrationSuspendTestHandler = IntegrationSuspendTestHandler()

    @Bean
    fun integrationBlockingTestHandler(): IntegrationBlockingTestHandler = IntegrationBlockingTestHandler()

    @Bean
    fun kafkaTemplate(embeddedKafkaBroker: EmbeddedKafkaBroker): KafkaTemplate<String, String> {
        val producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker).apply {
            put("acks", "1")
            put("retries", 0)
            put("linger.ms", 0)
        }
        return KafkaTemplate(DefaultKafkaProducerFactory(producerProps))
    }

    @Bean("kafkaListenerContainerFactory")
    fun kafkaListenerContainerFactory(
        embeddedKafkaBroker: EmbeddedKafkaBroker
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()

        val consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker).apply {
            put("auto.offset.reset", "earliest")
            put("enable.auto.commit", "false")
        }

        factory.consumerFactory = DefaultKafkaConsumerFactory(consumerProps)
        factory.setConcurrency(1)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE

        return factory
    }
}