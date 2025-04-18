package io.clroot.ball.adapter.outbound.messaging.producer.kafka

import io.clroot.ball.domain.event.DomainEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(
    classes = [TestConfig::class],
    properties = ["spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"]
)
@EmbeddedKafka(partitions = 1, topics = ["test-events", "no-id-test-events"])
class KafkaEventPublisherTest {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var kafkaEventPublisher: KafkaEventPublisher

    private lateinit var consumer: KafkaConsumer<String, String>

    @Value("\${spring.embedded.kafka.brokers}")
    private lateinit var embeddedKafkaBrokerAddress: String

    @BeforeEach
    fun setUp() {
        // Configure Kafka consumer
        val consumerProps = HashMap<String, Any>().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBrokerAddress)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-group")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        }
        consumer = KafkaConsumer<String, String>(consumerProps)
    }

    @AfterEach
    fun tearDown() {
        consumer.close()
    }

    @Test
    fun `should publish event to Kafka`() {
        // Given
        val testEvent = TestEvent(id = "test-id-123", message = "Test message", occurredAt = Instant.now())
        val expectedTopic = "test-events" // Based on the determineDestination logic

        // Subscribe to the topic
        consumer.subscribe(listOf(expectedTopic))

        // When
        kafkaEventPublisher.publish(testEvent)

        // Then
        val records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10))
        assertNotNull(records)

        val record = records.records(expectedTopic).first()
        assertNotNull(record)
        assertEquals("test-id-123", record.key())
        // In a real test, we would deserialize the value and verify its contents
        // For simplicity, we're just checking that we received a record
    }

    @Test
    fun `should use event class name as key when no id field exists`() {
        // Given
        val testEvent = NoIdTestEvent(message = "Test without ID", occurredAt = Instant.now())
        val expectedTopic = "no-id-test-events" // Based on the determineDestination logic

        // Subscribe to the topic
        consumer.subscribe(listOf(expectedTopic))

        // When
        kafkaEventPublisher.publish(testEvent)

        // Then
        val records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10))
        assertNotNull(records)

        val record = records.records(expectedTopic).first()
        assertNotNull(record)
        assertEquals("NoIdTestEvent", record.key()) // Should use class name as key
    }

    // Test event classes
    data class TestEvent(
        val id: String,
        val message: String,
        override val occurredAt: Instant
    ) : DomainEvent

    data class NoIdTestEvent(
        val message: String,
        override val occurredAt: Instant
    ) : DomainEvent
}
