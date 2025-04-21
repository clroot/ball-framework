package io.clroot.ball.adapter.inbound.messaging.kafka.config

import io.clroot.ball.adapter.inbound.messaging.core.messaging.MessageBrokerConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class KafkaConsumerPropertiesTest : FunSpec({
    
    test("KafkaConsumerProperties should have correct default values") {
        // When
        val properties = KafkaConsumerProperties()
        
        // Then
        properties.hosts shouldBe listOf("localhost:9092")
        properties.maxMessagesPerConnection shouldBe 10
        properties.consumerGroupIdPrefix shouldBe "ball-consumer"
        properties.defaultRetryCount shouldBe 3
        properties.enableDlq shouldBe true
        properties.additionalConfig shouldBe emptyMap()
        properties.brokerType shouldBe "kafka"
        
        // Kafka specific properties
        properties.enableAutoCommit shouldBe false
        properties.autoCommitIntervalMs shouldBe 5000
        properties.maxPollIntervalMs shouldBe 300000
        properties.sessionTimeoutMs shouldBe 30000
        properties.concurrency shouldBe 1
        properties.offsetReset shouldBe "earliest"
        properties.dlqSuffix shouldBe ".dlq"
    }
    
    test("KafkaConsumerProperties should implement MessageBrokerConfig") {
        // When
        val properties = KafkaConsumerProperties()
        
        // Then
        properties.shouldBeInstanceOf<MessageBrokerConfig>()
    }
    
    test("KafkaConsumerProperties should allow overriding default values") {
        // When
        val properties = KafkaConsumerProperties(
            hosts = listOf("kafka1:9092", "kafka2:9092"),
            maxMessagesPerConnection = 20,
            consumerGroupIdPrefix = "custom-consumer",
            defaultRetryCount = 5,
            enableDlq = false,
            additionalConfig = mapOf("key" to "value"),
            enableAutoCommit = true,
            autoCommitIntervalMs = 10000,
            maxPollIntervalMs = 600000,
            sessionTimeoutMs = 60000,
            concurrency = 3,
            offsetReset = "latest",
            dlqSuffix = ".dead-letter"
        )
        
        // Then
        properties.hosts shouldBe listOf("kafka1:9092", "kafka2:9092")
        properties.maxMessagesPerConnection shouldBe 20
        properties.consumerGroupIdPrefix shouldBe "custom-consumer"
        properties.defaultRetryCount shouldBe 5
        properties.enableDlq shouldBe false
        properties.additionalConfig shouldBe mapOf("key" to "value")
        
        // Kafka specific properties
        properties.enableAutoCommit shouldBe true
        properties.autoCommitIntervalMs shouldBe 10000
        properties.maxPollIntervalMs shouldBe 600000
        properties.sessionTimeoutMs shouldBe 60000
        properties.concurrency shouldBe 3
        properties.offsetReset shouldBe "latest"
        properties.dlqSuffix shouldBe ".dead-letter"
    }
})