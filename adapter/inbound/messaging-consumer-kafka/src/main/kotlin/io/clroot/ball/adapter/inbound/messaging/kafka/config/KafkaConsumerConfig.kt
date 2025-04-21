package io.clroot.ball.adapter.inbound.messaging.kafka.config

import io.clroot.ball.adapter.inbound.messaging.core.MessageConsumerRegistry
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

/**
 * Kafka 소비자 설정
 * Spring Boot 애플리케이션에서 Kafka 소비자를 설정하기 위한 구성
 */
@Configuration
@EnableConfigurationProperties(KafkaConsumerProperties::class)
class KafkaConsumerConfig(
    private val properties: KafkaConsumerProperties
) {
    /**
     * Kafka 소비자 팩토리 생성
     *
     * @return Kafka 소비자 팩토리
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val props = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to properties.hosts.joinToString(","),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to properties.enableAutoCommit,
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG to properties.autoCommitIntervalMs,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to properties.maxPollIntervalMs,
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to properties.sessionTimeoutMs,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to properties.offsetReset
        )

        // 추가 설정 적용
        props.putAll(properties.additionalConfig.mapValues { it.value })

        return DefaultKafkaConsumerFactory(props)
    }

    /**
     * Kafka 리스너 컨테이너 팩토리 생성
     *
     * @return Kafka 리스너 컨테이너 팩토리
     */
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(properties.concurrency)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    /**
     * 메시지 소비자 레지스트리 빈 생성
     *
     * @return 메시지 소비자 레지스트리
     */
    @Bean
    fun messageConsumerRegistry(): MessageConsumerRegistry {
        return MessageConsumerRegistry()
    }
}