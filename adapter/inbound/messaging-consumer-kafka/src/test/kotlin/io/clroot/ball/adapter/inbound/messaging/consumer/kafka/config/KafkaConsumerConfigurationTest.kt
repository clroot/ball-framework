package io.clroot.ball.adapter.inbound.messaging.consumer.kafka.config

import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.KafkaErrorHandlingProperties
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.KafkaEventConsumerProperties
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.apache.kafka.clients.consumer.ConsumerConfig

class KafkaConsumerConfigurationTest : BehaviorSpec({

    given("KafkaConsumerConfiguration") {
        `when`("기본 설정으로 Consumer Factory를 생성하는 경우") {
            val properties = KafkaEventConsumerProperties()
            val configuration = KafkaConsumerConfiguration(properties)

            then("Consumer Factory가 올바르게 생성되어야 한다") {
                val consumerFactory = configuration.consumerFactory()
                
                consumerFactory shouldNotBe null
                
                val configProps = consumerFactory.configurationProperties
                configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] shouldBe "localhost:9092"
                configProps[ConsumerConfig.GROUP_ID_CONFIG] shouldBe "ball-framework"
                configProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] shouldBe "earliest"
                configProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] shouldBe false
                configProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] shouldBe 500
            }
        }

        `when`("커스텀 설정으로 Consumer Factory를 생성하는 경우") {
            val customProperties = KafkaEventConsumerProperties(
                bootstrapServers = "kafka1:9092,kafka2:9092",
                groupId = "custom-group",
                autoOffsetReset = "latest",
                enableAutoCommit = true,
                maxPollRecords = 1000,
                fetchMinBytes = 1024,
                fetchMaxWaitMs = 2000,
                sessionTimeoutMs = 45000,
                heartbeatIntervalMs = 4000,
                maxPollIntervalMs = 600000
            )
            val configuration = KafkaConsumerConfiguration(customProperties)

            then("커스텀 설정이 반영된 Consumer Factory가 생성되어야 한다") {
                val consumerFactory = configuration.consumerFactory()
                
                val configProps = consumerFactory.configurationProperties
                configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] shouldBe "kafka1:9092,kafka2:9092"
                configProps[ConsumerConfig.GROUP_ID_CONFIG] shouldBe "custom-group"
                configProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] shouldBe "latest"
                configProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] shouldBe true
                configProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] shouldBe 1000
                configProps[ConsumerConfig.FETCH_MIN_BYTES_CONFIG] shouldBe 1024
                configProps[ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG] shouldBe 2000
                configProps[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] shouldBe 45000
                configProps[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] shouldBe 4000
                configProps[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] shouldBe 600000
            }
        }

        `when`("Kafka Listener Container Factory를 생성하는 경우") {
            val properties = KafkaEventConsumerProperties(
                concurrency = 5,
                enableAutoCommit = false
            )
            val configuration = KafkaConsumerConfiguration(properties)

//            then("설정된 동시성과 컨테이너 속성을 가진 Factory가 생성되어야 한다") {
//                val factory = configuration.kafkaListenerContainerFactory()
//
//                factory shouldNotBe null
//
//                val containerProperties = factory.containerProperties
//                containerProperties.ackMode shouldBe ContainerProperties.AckMode.MANUAL_IMMEDIATE
//            }
        }

        `when`("Auto Commit이 활성화된 경우") {
            val properties = KafkaEventConsumerProperties(
                enableAutoCommit = true,
                concurrency = 2
            )
            val configuration = KafkaConsumerConfiguration(properties)

//            then("BATCH Acknowledgment 모드가 설정되어야 한다") {
//                val factory = configuration.kafkaListenerContainerFactory()
//
//                val containerProperties = factory.containerProperties
//                containerProperties.ackMode shouldBe ContainerProperties.AckMode.BATCH
//            }
        }

        `when`("클라이언트 ID 설정을 확인하는 경우") {
            val properties = KafkaEventConsumerProperties(
                groupId = "test-group"
            )
            val configuration = KafkaConsumerConfiguration(properties)

            then("Group ID를 포함한 고유한 클라이언트 ID가 생성되어야 한다") {
                val consumerFactory = configuration.consumerFactory()
                val configProps = consumerFactory.configurationProperties
                val clientId = configProps[ConsumerConfig.CLIENT_ID_CONFIG] as String
                
                clientId shouldContain "test-group"
                clientId.length shouldBeGreaterThan "test-group".length // UUID가 추가되어야 함
            }
        }

        `when`("Deserializer 설정을 확인하는 경우") {
            val properties = KafkaEventConsumerProperties()
            val configuration = KafkaConsumerConfiguration(properties)

            then("ErrorHandlingDeserializer가 설정되어야 한다") {
                val consumerFactory = configuration.consumerFactory()
                val configProps = consumerFactory.configurationProperties
                
                configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] shouldBe 
                    org.springframework.kafka.support.serializer.ErrorHandlingDeserializer::class.java
                configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] shouldBe 
                    org.springframework.kafka.support.serializer.ErrorHandlingDeserializer::class.java
            }
        }

        `when`("추가 성능 최적화 설정을 확인하는 경우") {
            val properties = KafkaEventConsumerProperties()
            val configuration = KafkaConsumerConfiguration(properties)

            then("성능 최적화 관련 설정들이 올바르게 구성되어야 한다") {
                val consumerFactory = configuration.consumerFactory()
                val configProps = consumerFactory.configurationProperties
                
                configProps[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] shouldBe false
                configProps[ConsumerConfig.CHECK_CRCS_CONFIG] shouldBe true
                
                val partitionAssignmentStrategy = configProps[ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG] as List<*>
                partitionAssignmentStrategy.size shouldBe 2
                partitionAssignmentStrategy shouldContain "org.apache.kafka.clients.consumer.RoundRobinAssignor"
                partitionAssignmentStrategy shouldContain "org.apache.kafka.clients.consumer.RangeAssignor"
            }
        }
    }

    given("Kafka Error Handling Properties") {
        `when`("커스텀 에러 핸들링 설정을 사용하는 경우") {
            val customErrorHandling = KafkaErrorHandlingProperties(
                commitRetryAttempts = 5,
                commitRetryDelayMs = 200,
                rebalanceTimeoutMs = 60000,
                consumerRestartDelayMs = 10000
            )
            val properties = KafkaEventConsumerProperties(
                kafkaErrorHandling = customErrorHandling
            )
            val configuration = KafkaConsumerConfiguration(properties)

//            then("커스텀 에러 핸들링 설정이 반영되어야 한다") {
//                val factory = configuration.kafkaListenerContainerFactory()
//                // 실제 설정 검증은 통합 테스트에서 수행
//                factory shouldNotBe null
//            }
        }
    }
})

/**
 * 확장 함수로 테스트 유틸리티 추가
 */
private infix fun Int.shouldBeGreaterThan(other: Int) {
    if (this <= other) {
        throw AssertionError("Expected $this to be greater than $other")
    }
}
