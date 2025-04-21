package io.clroot.ball.adapter.inbound.messaging.kafka.exception

import io.clroot.ball.adapter.inbound.messaging.core.MessageDispatchException
import org.apache.kafka.clients.consumer.ConsumerRecord

/**
 * Kafka 메시지 처리 중 발생하는 예외
 *
 * @property record Kafka 소비자 레코드
 * @property messageId 메시지 ID
 * @property topic 메시지 토픽
 * @property retryable 재시도 가능 여부
 */
class KafkaProcessingException(
    message: String,
    cause: Throwable? = null,
    messageId: String? = null,
    topic: String? = null,
    retryable: Boolean = true,
    val record: ConsumerRecord<*, *>? = null
) : MessageDispatchException(
    message = message,
    cause = cause,
    messageId = messageId,
    topic = topic,
    retryable = retryable
) {
    /**
     * 재시도 불가능한 예외 생성
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     * @param messageId 메시지 ID
     * @param topic 메시지 토픽
     * @param record Kafka 소비자 레코드
     * @return 재시도 불가능한 KafkaProcessingException 인스턴스
     */
    companion object {
        fun nonRetryable(
            message: String,
            cause: Throwable? = null,
            messageId: String? = null,
            topic: String? = null,
            record: ConsumerRecord<*, *>? = null
        ): KafkaProcessingException {
            return KafkaProcessingException(
                message = message,
                cause = cause,
                messageId = messageId,
                topic = topic,
                retryable = false,
                record = record
            )
        }
    }
}