package io.clroot.ball.adapter.inbound.messaging.consumer.kafka.listener

import io.clroot.ball.adapter.inbound.messaging.consumer.core.executor.DomainEventHandlerExecutor
import io.clroot.ball.adapter.inbound.messaging.consumer.core.listener.AbstractEventListener
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.KafkaEventConsumerProperties
import io.clroot.ball.adapter.inbound.messaging.consumer.kafka.converter.DomainEventKafkaMessageConverter
import io.clroot.ball.domain.event.DomainEvent
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Kafka 도메인 이벤트 리스너
 *
 * Kafka 토픽에서 도메인 이벤트 메시지를 수신하고,
 * core 모듈의 공통 로직을 사용하여 등록된 핸들러들에게 전달합니다.
 *
 * 주요 특징:
 * - Kafka Consumer 기반 이벤트 수신
 * - AbstractEventListener 상속으로 공통 로직 활용
 * - 메시지 변환 및 유효성 검증
 * - 수동 오프셋 커밋으로 메시지 손실 방지
 * - DLQ(Dead Letter Queue) 지원
 *
 * 이 클래스는 Auto Configuration에 의해 자동으로 등록됩니다.
 */
@Component
class KafkaEventListener(
    handlerExecutor: DomainEventHandlerExecutor,
    private val kafkaProperties: KafkaEventConsumerProperties,
    private val messageConverter: DomainEventKafkaMessageConverter,
) : AbstractEventListener(handlerExecutor, kafkaProperties) {
    
    private val kafkaLog = LoggerFactory.getLogger(javaClass)

    /**
     * Kafka 토픽에서 도메인 이벤트 메시지 수신
     * 
     * @KafkaListener를 통해 설정된 토픽들로부터 메시지를 받고,
     * 부모 클래스의 공통 로직을 사용하여 처리합니다.
     */
    @KafkaListener(
        topics = ["\${ball.event.consumer.kafka.topics}"],
        groupId = "\${ball.event.consumer.kafka.groupId}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleKafkaMessage(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment?
    ) {
        kafkaLog.debug("Received Kafka message from topic: {}, partition: {}, offset: {}", 
            topic, partition, offset)

        try {
            // 메시지 유효성 검증
            if (!messageConverter.isValidMessage(message)) {
                kafkaLog.warn("Invalid message format received from Kafka: {}", message)
                acknowledgment?.acknowledge() // 잘못된 형식 메시지는 스킵
                return
            }

            // 도메인 이벤트로 변환
            val domainEvent = messageConverter.convertToDomainEvent(message)
            if (domainEvent == null) {
                kafkaLog.warn("Failed to convert Kafka message to domain event: {}", message)
                handleUnprocessableMessage(message, topic, partition, offset, acknowledgment)
                return
            }

            kafkaLog.debug("Kafka consumer received domain event: {} (ID: {})", 
                domainEvent.type, domainEvent.id)

            // 부모 클래스의 공통 처리 로직 호출
            if (kafkaProperties.async) {
                // 비동기 처리
                processEvent(domainEvent)
                acknowledgment?.acknowledge() // 즉시 커밋 (비동기 처리)
            } else {
                // 동기 처리 - 처리 완료 후 커밋
                runBlocking {
                    handlerExecutor.execute(domainEvent)
                }
                acknowledgment?.acknowledge()
            }

            kafkaLog.debug("Successfully processed Kafka message from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset)

        } catch (e: Exception) {
            kafkaLog.error("Failed to process Kafka message from topic: {}, partition: {}, offset: {}", 
                topic, partition, offset, e)
            
            handleKafkaProcessingError(message, topic, partition, offset, acknowledgment, e)
        }
    }

    /**
     * Kafka 특화 에러 핸들링
     * 
     * Kafka 메시지 처리 실패 시의 특화된 에러 처리를 수행합니다.
     */
    private fun handleKafkaProcessingError(
        message: String,
        topic: String,
        partition: Int,
        offset: Long,
        acknowledgment: Acknowledgment?,
        error: Exception
    ) {
        kafkaLog.error("Kafka message processing failed - Topic: {}, Partition: {}, Offset: {}, Message: {}", 
            topic, partition, offset, message, error)

        // DLQ 전송 (실제 구현은 별도의 DLQ Producer 필요)
        if (kafkaProperties.enableDlq) {
            sendToDeadLetterQueue(message, topic, partition, offset, error)
        }

        // 에러 발생 시에도 오프셋은 커밋 (무한 재처리 방지)
        acknowledgment?.acknowledge()
    }

    /**
     * 처리할 수 없는 메시지 핸들링
     * 
     * 메시지 형식이 잘못되었거나 변환에 실패한 경우의 처리입니다.
     */
    private fun handleUnprocessableMessage(
        message: String,
        topic: String,
        partition: Int,
        offset: Long,
        acknowledgment: Acknowledgment?
    ) {
        kafkaLog.warn("Unprocessable message - Topic: {}, Partition: {}, Offset: {}, Message: {}", 
            topic, partition, offset, message)

        // DLQ로 전송
        if (kafkaProperties.enableDlq) {
            val error = IllegalArgumentException("Unprocessable message format")
            sendToDeadLetterQueue(message, topic, partition, offset, error)
        }

        // 처리할 수 없는 메시지도 스킵
        acknowledgment?.acknowledge()
    }

    /**
     * Dead Letter Queue로 메시지 전송
     * 
     * 처리에 실패한 메시지를 DLQ로 전송합니다.
     * 실제 구현에서는 별도의 Kafka Producer가 필요합니다.
     */
    private fun sendToDeadLetterQueue(
        originalMessage: String,
        originalTopic: String,
        partition: Int,
        offset: Long,
        error: Exception
    ) {
        try {
            kafkaLog.info("Sending message to DLQ - Original Topic: {}, Partition: {}, Offset: {}", 
                originalTopic, partition, offset)

            // DLQ 메시지 구성
            val dlqMessage = createDlqMessage(originalMessage, originalTopic, partition, offset, error)
            
            // TODO: 실제 DLQ Producer를 통해 메시지 전송
            // kafkaDlqProducer.send(kafkaProperties.dlqTopic, dlqMessage)
            
            kafkaLog.debug("DLQ message created for failed processing: {}", dlqMessage)

        } catch (dlqError: Exception) {
            kafkaLog.error("Failed to send message to DLQ: {}", originalMessage, dlqError)
        }
    }

    /**
     * DLQ 메시지 생성
     * 
     * 원본 메시지와 에러 정보를 포함한 DLQ 메시지를 생성합니다.
     */
    private fun createDlqMessage(
        originalMessage: String,
        originalTopic: String,
        partition: Int,
        offset: Long,
        error: Exception
    ): String {
        val dlqData = mapOf(
            "originalMessage" to originalMessage,
            "originalTopic" to originalTopic,
            "originalPartition" to partition,
            "originalOffset" to offset,
            "errorType" to error.javaClass.simpleName,
            "errorMessage" to (error.message ?: "Unknown error"),
            "processedAt" to System.currentTimeMillis(),
            "consumerGroupId" to kafkaProperties.groupId
        )

        // JSON으로 직렬화
        return try {
            com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dlqData)
        } catch (e: Exception) {
            kafkaLog.warn("Failed to serialize DLQ message, using fallback format", e)
            """{"error": "DLQ serialization failed", "originalMessage": "$originalMessage"}"""
        }
    }

    /**
     * Kafka 전용 에러 핸들링 (부모 클래스 메서드 확장)
     */
    override fun handleEventError(event: DomainEvent, error: Exception) {
        kafkaLog.debug("Kafka specific error handling for event {} (ID: {})", event.type, event.id)

        // 부모 클래스의 기본 에러 처리 호출
        super.handleEventError(event, error)

        // Kafka 전용 에러 처리 로직
        // 예: Kafka 메트릭 수집, Kafka 특화 알림 등
    }

    /**
     * 리스너 종료 시 정리 작업
     */
    override fun shutdown() {
        kafkaLog.info("Shutting down Kafka event listener...")
        super.shutdown()
        kafkaLog.info("Kafka event listener shutdown completed")
    }
}