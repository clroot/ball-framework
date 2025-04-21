package io.clroot.ball.adapter.inbound.messaging.kafka.config

import io.clroot.ball.adapter.inbound.messaging.core.MessageConsumerRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

class KafkaConsumerConfigTest : FunSpec({
    
    test("consumerFactory should create a DefaultKafkaConsumerFactory with correct properties") {
        // Given
        val properties = KafkaConsumerProperties(
            hosts = listOf("kafka1:9092", "kafka2:9092"),
            enableAutoCommit = true,
            autoCommitIntervalMs = 10000,
            maxPollIntervalMs = 600000,
            sessionTimeoutMs = 60000,
            offsetReset = "latest",
            additionalConfig = mapOf("custom.property" to "custom-value")
        )
        val config = KafkaConsumerConfig(properties)
        
        // When
        val factory = config.consumerFactory()
        
        // Then
        factory.shouldBeInstanceOf<DefaultKafkaConsumerFactory<String, String>>()
        
        // Access the configs via reflection since they're private
        val configsField = DefaultKafkaConsumerFactory::class.java.getDeclaredField("configs")
        configsField.isAccessible = true
        val configs = configsField.get(factory) as Map<String, Any>
        
        configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] shouldBe "kafka1:9092,kafka2:9092"
        configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] shouldBe StringDeserializer::class.java
        configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] shouldBe StringDeserializer::class.java
        configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] shouldBe true
        configs[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] shouldBe 10000
        configs[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] shouldBe 600000
        configs[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] shouldBe 60000
        configs[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] shouldBe "latest"
        configs["custom.property"] shouldBe "custom-value"
    }
    
    test("kafkaListenerContainerFactory should create a factory with correct properties") {
        // Given
        val properties = KafkaConsumerProperties(concurrency = 3)
        val config = KafkaConsumerConfig(properties)
        
        // When
        val factory = config.kafkaListenerContainerFactory()
        
        // Then
        factory.shouldBeInstanceOf<ConcurrentKafkaListenerContainerFactory<String, String>>()
        
        // Access the concurrency via reflection since it's private
        val concurrencyField = ConcurrentKafkaListenerContainerFactory::class.java.getDeclaredField("concurrency")
        concurrencyField.isAccessible = true
        val concurrency = concurrencyField.get(factory) as Int
        
        concurrency shouldBe 3
        factory.containerProperties.ackMode shouldBe ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.consumerFactory shouldNotBe null
    }
    
    test("messageConsumerRegistry should create a MessageConsumerRegistry") {
        // Given
        val config = KafkaConsumerConfig(KafkaConsumerProperties())
        
        // When
        val registry = config.messageConsumerRegistry()
        
        // Then
        registry.shouldBeInstanceOf<MessageConsumerRegistry>()
    }
})