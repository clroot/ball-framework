package io.clroot.ball.adapter.outbound.messaging.producer.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Test configuration for Kafka tests using Spring's embedded Kafka
 */
@TestConfiguration
@SpringBootApplication
@EnableKafka
class TestConfig {

    @Value("\${spring.embedded.kafka.brokers}")
    private lateinit var embeddedKafkaBrokerAddress: String

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configProps = HashMap<String, Any>()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = embeddedKafkaBrokerAddress
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun kafkaEventPublisher(kafkaTemplate: KafkaTemplate<String, Any>): KafkaEventPublisher {
        return KafkaEventPublisher(kafkaTemplate)
    }
}
