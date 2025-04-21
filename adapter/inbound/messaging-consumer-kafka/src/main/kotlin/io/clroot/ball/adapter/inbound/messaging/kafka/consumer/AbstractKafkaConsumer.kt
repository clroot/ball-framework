package io.clroot.ball.adapter.inbound.messaging.kafka.consumer

import io.clroot.ball.adapter.inbound.messaging.core.AbstractMessageConsumer
import io.clroot.ball.adapter.inbound.messaging.core.MessageMetadata
import io.clroot.ball.adapter.inbound.messaging.core.retry.MessageRetryHandler
import io.clroot.ball.adapter.inbound.messaging.kafka.exception.KafkaProcessingException
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import java.time.Instant

/**
 * Spring Kafka 리스너와 Ball Framework의 메시지 소비자를 연결하는 어댑터
 *
 * @param P 메시지 페이로드 타입
 * @param D 변환된 도메인 객체 타입
 */
abstract class AbstractKafkaConsumer<P, D> : AbstractMessageConsumer<P, D>() {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kafka 메시지 리스너
     * Spring Kafka의 @KafkaListener 애노테이션을 사용하여 메시지 수신
     *
     * @param payload 메시지 페이로드
     * @param topic 메시지 토픽
     * @param partition 메시지 파티션
     * @param offset 메시지 오프셋
     * @param timestamp 메시지 타임스탬프
     * @param acknowledgment 메시지 확인 객체
     * @param headers 메시지 헤더
     * @param record 원본 Kafka 레코드
     */
    @KafkaListener(
        topics = ["#{__listener.getTopicName()}"],
        groupId = "#{__listener.getConsumerGroupId()}"
    )
    fun listen(
        @Payload payload: P,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment,
        @Header("kafka_receivedHeaders", required = false) headers: Map<String, Any>? = null,
        @Header("kafka_receivedRecord", required = false) record: ConsumerRecord<*, *>? = null
    ) {
        val messageId = "$topic-$partition-$offset"
        log.debug("Received Kafka message: id={}, topic={}, partition={}, offset={}", messageId, topic, partition, offset)

        try {
            // 메시지 메타데이터 생성
            val metadata = MessageMetadata(
                messageId = messageId,
                timestamp = Instant.ofEpochMilli(timestamp),
                headers = headers?.mapValues { it.value.toString() } ?: emptyMap(),
                source = "kafka",
                eventType = extractEventType(headers, payload)
            )

            // 재시도 핸들러를 통한 메시지 처리
            val retryHandler = MessageRetryHandler(this)
            val result = runBlocking {
                retryHandler.executeWithRetry(payload, metadata)
            }

            // 처리 결과에 따른 메시지 확인 또는 오류 처리
            result.fold(
                { error ->
                    // 오류 발생 시 처리
                    handleProcessingError(error, payload, metadata, record)
                    // 오류가 발생해도 메시지는 처리된 것으로 간주하고 확인
                    acknowledgment.acknowledge()
                },
                {
                    // 성공 시 메시지 확인
                    acknowledgment.acknowledge()
                    log.debug("Successfully processed Kafka message: id={}, topic={}", messageId, topic)
                }
            )
        } catch (e: Exception) {
            // 예상치 못한 예외 발생 시 처리
            log.error("Unexpected error processing Kafka message: id={}, topic={}", messageId, topic, e)

            // 예외 유형에 따라 메시지 확인 여부 결정
            if (shouldAcknowledgeOnError(e)) {
                acknowledgment.acknowledge()
            }

            // 예외 재발생 (Spring Kafka의 오류 핸들러가 처리)
            throw KafkaProcessingException(
                message = "Failed to process Kafka message: ${e.message}",
                cause = e,
                messageId = messageId,
                topic = topic,
                record = record
            )
        }
    }

    /**
     * 오류 발생 시 메시지 확인 여부 결정
     * 기본적으로는 모든 예외에 대해 메시지를 확인하지 않음 (재처리 가능하도록)
     * 하위 클래스에서 필요에 따라 재정의 가능
     *
     * @param error 발생한 예외
     * @return 메시지 확인 여부
     */
    protected open fun shouldAcknowledgeOnError(error: Throwable): Boolean {
        return false
    }

    /**
     * 메시지 처리 오류 처리
     * 기본적으로는 로깅만 수행하지만, 하위 클래스에서 재정의 가능
     *
     * @param error 발생한 오류
     * @param payload 원본 메시지 페이로드
     * @param metadata 메시지 메타데이터
     * @param record 원본 Kafka 레코드
     */
    protected open fun handleProcessingError(
        error: Any,
        payload: P,
        metadata: MessageMetadata,
        record: ConsumerRecord<*, *>?
    ) {
        log.error(
            "Error processing Kafka message: id={}, topic={}, error={}",
            metadata.messageId, getTopicName(), error
        )
    }

    /**
     * 이벤트 타입 추출
     * 기본적으로는 헤더에서 'event-type' 키를 찾거나, 페이로드 클래스 이름을 사용
     * 하위 클래스에서 필요에 따라 재정의 가능
     *
     * @param headers 메시지 헤더
     * @param payload 메시지 페이로드
     * @return 이벤트 타입
     */
    protected open fun extractEventType(headers: Map<String, Any>?, payload: P): String {
        // 헤더에서 이벤트 타입 찾기
        headers?.get("event-type")?.toString()?.let { return it }

        // 페이로드가 null이 아니면 클래스 이름 사용
        if (payload != null) {
            return payload.javaClass.simpleName
        }

        // 기본값
        return "unknown"
    }

    /**
     * 소비자 그룹 ID 반환
     * 하위 클래스에서 반드시 구현해야 함
     *
     * @return 소비자 그룹 ID
     */
    abstract fun getConsumerGroupId(): String
}
