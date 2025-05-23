package io.clroot.ball.adapter.inbound.messaging.consumer.kafka.config

import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.KafkaEventConsumerProperties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import java.util.*

/**
 * Kafka Consumer 설정
 * 
 * Kafka Consumer의 기본 설정과 리스너 컨테이너 팩토리를 구성합니다.
 * 
 * 주요 설정:
 * - Consumer Properties 구성
 * - Deserializer 설정
 * - 에러 핸들링 설정
 * - 컨테이너 팩토리 설정
 */
@Configuration
class KafkaConsumerConfiguration(
    private val kafkaProperties: KafkaEventConsumerProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kafka Consumer Factory 생성
     * 
     * Kafka Consumer의 기본 설정을 구성합니다.
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val configProps = createConsumerProperties()
        
        log.info("Creating Kafka ConsumerFactory with properties: {}", 
            configProps.filterKeys { !it.contains("password", ignoreCase = true) })
        
        return DefaultKafkaConsumerFactory(configProps)
    }

    /**
     * Kafka Listener Container Factory 생성
     * 
     * @KafkaListener 어노테이션을 위한 컨테이너 팩토리를 구성합니다.
     */
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        
        // Consumer Factory 설정
        factory.consumerFactory = consumerFactory()
        
        // 동시성 설정 (파티션별 컨슈머 스레드 수)
        factory.setConcurrency(kafkaProperties.concurrency)
        
        // Container Properties 설정
        configureContainerProperties(factory.containerProperties)
        
        log.info("Created Kafka ListenerContainerFactory with concurrency: {}", kafkaProperties.concurrency)
        
        return factory
    }

    /**
     * Kafka Consumer Properties 생성
     * 
     * Kafka Consumer의 세부 설정을 구성합니다.
     */
    private fun createConsumerProperties(): Map<String, Any> {
        val properties = mutableMapOf<String, Any>()
        
        // 기본 연결 설정
        properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        properties[ConsumerConfig.GROUP_ID_CONFIG] = kafkaProperties.groupId
        
        // Deserializer 설정 (에러 핸들링 포함)
        properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        properties[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = StringDeserializer::class.java
        properties[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = StringDeserializer::class.java
        
        // 오프셋 관리 설정
        properties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = kafkaProperties.autoOffsetReset
        properties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = kafkaProperties.enableAutoCommit
        
        // 성능 관련 설정
        properties[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = kafkaProperties.maxPollRecords
        properties[ConsumerConfig.FETCH_MIN_BYTES_CONFIG] = kafkaProperties.fetchMinBytes
        properties[ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG] = kafkaProperties.fetchMaxWaitMs
        
        // 세션 관리 설정
        properties[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = kafkaProperties.sessionTimeoutMs
        properties[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = kafkaProperties.heartbeatIntervalMs
        properties[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = kafkaProperties.maxPollIntervalMs
        
        // 클라이언트 식별
        properties[ConsumerConfig.CLIENT_ID_CONFIG] = "${kafkaProperties.groupId}-${UUID.randomUUID()}"
        
        // 추가 성능 최적화 설정
        properties[ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG] = false
        properties[ConsumerConfig.CHECK_CRCS_CONFIG] = true
        properties[ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG] = listOf(
            "org.apache.kafka.clients.consumer.RoundRobinAssignor",
            "org.apache.kafka.clients.consumer.RangeAssignor"
        )
        
        return properties
    }

    /**
     * Container Properties 설정
     * 
     * Kafka Listener Container의 동작 방식을 구성합니다.
     */
    private fun configureContainerProperties(containerProperties: ContainerProperties) {
        // Acknowledgment 모드 설정 (수동 커밋)
        containerProperties.ackMode = if (kafkaProperties.enableAutoCommit) {
            ContainerProperties.AckMode.BATCH
        } else {
            ContainerProperties.AckMode.MANUAL_IMMEDIATE
        }
        
        // 에러 핸들링 설정
        containerProperties.isLogContainerConfig = true
        
        // 리밸런싱 설정
        containerProperties.consumerRebalanceListener = createRebalanceListener()
        
        // 종료 시 처리 중인 메시지 완료 대기 시간
        containerProperties.shutdownTimeout = kafkaProperties.kafkaErrorHandling.rebalanceTimeoutMs
        
        log.debug("Configured container properties - AckMode: {}, ShutdownTimeout: {}ms", 
            containerProperties.ackMode, containerProperties.shutdownTimeout)
    }

    /**
     * Consumer Rebalance Listener 생성
     * 
     * 파티션 재할당 시의 동작을 정의합니다.
     */
    private fun createRebalanceListener(): org.apache.kafka.clients.consumer.ConsumerRebalanceListener {
        return object : org.apache.kafka.clients.consumer.ConsumerRebalanceListener {
            override fun onPartitionsRevoked(partitions: Collection<org.apache.kafka.common.TopicPartition>) {
                log.info("Consumer partitions revoked: {}", partitions)
                
                // 파티션 해제 시 정리 작업
                // 예: 처리 중인 메시지 완료 대기, 리소스 정리 등
            }

            override fun onPartitionsAssigned(partitions: Collection<org.apache.kafka.common.TopicPartition>) {
                log.info("Consumer partitions assigned: {}", partitions)
                
                // 파티션 할당 시 초기화 작업
                // 예: 메트릭 초기화, 모니터링 설정 등
            }
        }
    }
}
