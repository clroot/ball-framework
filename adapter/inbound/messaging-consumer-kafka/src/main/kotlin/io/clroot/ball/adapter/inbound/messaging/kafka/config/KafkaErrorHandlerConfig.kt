package io.clroot.ball.adapter.inbound.messaging.kafka.config

import io.clroot.ball.adapter.inbound.messaging.kafka.exception.KafkaProcessingException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.BackOff
import org.springframework.util.backoff.ExponentialBackOff

/**
 * Kafka 오류 처리 핸들러 설정
 */
@Configuration
class KafkaErrorHandlerConfig(
    private val properties: KafkaConsumerProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 백오프 정책 생성
     *
     * @return 지수 백오프 정책
     */
    @Bean
    fun backOff(): BackOff {
        val backOff = ExponentialBackOff(1000L, 2.0)
        backOff.maxAttempts = properties.defaultRetryCount
        return backOff
    }

    /**
     * Kafka 오류 핸들러 생성
     *
     * @param backOff 백오프 정책
     * @return Kafka 오류 핸들러
     */
    @Bean
    fun kafkaErrorHandler(backOff: BackOff): DefaultErrorHandler {
        val errorHandler = DefaultErrorHandler({ record, exception ->
            log.error(
                "Failed to process Kafka record after all retries. Topic: {}, Partition: {}, Offset: {}, Key: {}, Exception: {}",
                record.topic(), record.partition(), record.offset(), record.key(), exception.message, exception
            )

            // DLQ 처리 로직을 여기에 추가할 수 있음
            if (properties.enableDlq) {
                val dlqTopic = record.topic() + properties.dlqSuffix
                log.info("Sending failed message to DLQ topic: {}", dlqTopic)
                // DLQ로 메시지 전송 로직 (실제 구현은 별도 클래스로 분리 가능)
            }
        }, backOff)

        // 재시도하지 않을 예외 유형 설정
        errorHandler.addNotRetryableExceptions(
            KafkaProcessingException::class.java
        )

        return errorHandler
    }
}
