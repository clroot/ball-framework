package io.clroot.ball.adapter.inbound.messaging.consumer.kafka

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class KafkaEventConsumerPropertiesTest : BehaviorSpec({

    given("KafkaEventConsumerProperties") {
        `when`("기본 설정을 사용하는 경우") {
            val properties = KafkaEventConsumerProperties()

            then("기본값들이 올바르게 설정되어야 한다") {
                // 공통 설정 (EventConsumerProperties 상속)
                properties.enabled shouldBe true
                properties.async shouldBe true
                properties.parallel shouldBe true
                properties.maxConcurrency shouldBe 10
                properties.timeoutMs shouldBe 30000
                properties.enableRetry shouldBe true
                properties.maxRetryAttempts shouldBe 3
                properties.retryDelayMs shouldBe 1000

                // Kafka 전용 설정
                properties.topics shouldBe listOf("domain-events")
                properties.groupId shouldBe "ball-framework"
                properties.bootstrapServers shouldBe "localhost:9092"
                properties.autoOffsetReset shouldBe "earliest"
                properties.enableAutoCommit shouldBe false
                properties.maxPollRecords shouldBe 500
                properties.fetchMinBytes shouldBe 1
                properties.fetchMaxWaitMs shouldBe 500
                properties.sessionTimeoutMs shouldBe 30000
                properties.heartbeatIntervalMs shouldBe 3000
                properties.maxPollIntervalMs shouldBe 300000
                properties.concurrency shouldBe 3
                properties.enableDlq shouldBe true
                properties.dlqTopic shouldBe "domain-events-dlq"
            }
        }

        `when`("커스텀 설정을 사용하는 경우") {
            val customKafkaErrorHandling = KafkaErrorHandlingProperties(
                commitRetryAttempts = 5,
                commitRetryDelayMs = 200,
                rebalanceTimeoutMs = 60000,
                consumerRestartDelayMs = 10000
            )

            val properties = KafkaEventConsumerProperties(
                enabled = false,
                async = false,
                parallel = false,
                maxConcurrency = 5,
                timeoutMs = 15000,
                enableRetry = false,
                maxRetryAttempts = 5,
                retryDelayMs = 500,
                topics = listOf("custom-topic-1", "custom-topic-2"),
                groupId = "custom-group",
                bootstrapServers = "kafka1:9092,kafka2:9092",
                autoOffsetReset = "latest",
                enableAutoCommit = true,
                maxPollRecords = 1000,
                fetchMinBytes = 1024,
                fetchMaxWaitMs = 1000,
                sessionTimeoutMs = 60000,
                heartbeatIntervalMs = 5000,
                maxPollIntervalMs = 600000,
                concurrency = 5,
                enableDlq = false,
                dlqTopic = "custom-dlq",
                kafkaErrorHandling = customKafkaErrorHandling
            )

            then("설정값들이 올바르게 반영되어야 한다") {
                // 공통 설정 확인
                properties.enabled shouldBe false
                properties.async shouldBe false
                properties.parallel shouldBe false
                properties.maxConcurrency shouldBe 5
                properties.timeoutMs shouldBe 15000
                properties.enableRetry shouldBe false
                properties.maxRetryAttempts shouldBe 5
                properties.retryDelayMs shouldBe 500

                // Kafka 전용 설정 확인
                properties.topics shouldBe listOf("custom-topic-1", "custom-topic-2")
                properties.groupId shouldBe "custom-group"
                properties.bootstrapServers shouldBe "kafka1:9092,kafka2:9092"
                properties.autoOffsetReset shouldBe "latest"
                properties.enableAutoCommit shouldBe true
                properties.maxPollRecords shouldBe 1000
                properties.fetchMinBytes shouldBe 1024
                properties.fetchMaxWaitMs shouldBe 1000
                properties.sessionTimeoutMs shouldBe 60000
                properties.heartbeatIntervalMs shouldBe 5000
                properties.maxPollIntervalMs shouldBe 600000
                properties.concurrency shouldBe 5
                properties.enableDlq shouldBe false
                properties.dlqTopic shouldBe "custom-dlq"

                // Kafka 에러 핸들링 설정 확인
                properties.kafkaErrorHandling.commitRetryAttempts shouldBe 5
                properties.kafkaErrorHandling.commitRetryDelayMs shouldBe 200
                properties.kafkaErrorHandling.rebalanceTimeoutMs shouldBe 60000
                properties.kafkaErrorHandling.consumerRestartDelayMs shouldBe 10000
            }
        }

        `when`("부분적으로 커스텀 설정을 사용하는 경우") {
            val properties = KafkaEventConsumerProperties(
                topics = listOf("user-events", "order-events"),
                groupId = "custom-service",
                maxPollRecords = 100,
                concurrency = 1
            )

            then("지정된 값은 변경되고 나머지는 기본값이어야 한다") {
                // 변경된 값들
                properties.topics shouldBe listOf("user-events", "order-events")
                properties.groupId shouldBe "custom-service"
                properties.maxPollRecords shouldBe 100
                properties.concurrency shouldBe 1

                // 기본값들
                properties.enabled shouldBe true
                properties.bootstrapServers shouldBe "localhost:9092"
                properties.autoOffsetReset shouldBe "earliest"
                properties.enableAutoCommit shouldBe false
                properties.enableDlq shouldBe true
                properties.dlqTopic shouldBe "domain-events-dlq"
            }
        }

        `when`("여러 토픽을 설정한 경우") {
            val topics = listOf("topic1", "topic2", "topic3")
            val properties = KafkaEventConsumerProperties(topics = topics)

            then("모든 토픽이 포함되어야 한다") {
                properties.topics.size shouldBe 3
                properties.topics shouldContain "topic1"
                properties.topics shouldContain "topic2"
                properties.topics shouldContain "topic3"
            }
        }
    }

    given("KafkaErrorHandlingProperties") {
        `when`("기본 설정을 사용하는 경우") {
            val kafkaErrorHandling = KafkaErrorHandlingProperties()

            then("기본값들이 올바르게 설정되어야 한다") {
                kafkaErrorHandling.commitRetryAttempts shouldBe 3
                kafkaErrorHandling.commitRetryDelayMs shouldBe 100
                kafkaErrorHandling.rebalanceTimeoutMs shouldBe 30000
                kafkaErrorHandling.consumerRestartDelayMs shouldBe 5000
            }
        }

        `when`("커스텀 설정을 사용하는 경우") {
            val kafkaErrorHandling = KafkaErrorHandlingProperties(
                commitRetryAttempts = 10,
                commitRetryDelayMs = 500,
                rebalanceTimeoutMs = 120000,
                consumerRestartDelayMs = 15000
            )

            then("설정값들이 올바르게 반영되어야 한다") {
                kafkaErrorHandling.commitRetryAttempts shouldBe 10
                kafkaErrorHandling.commitRetryDelayMs shouldBe 500
                kafkaErrorHandling.rebalanceTimeoutMs shouldBe 120000
                kafkaErrorHandling.consumerRestartDelayMs shouldBe 15000
            }
        }
    }
})
