package io.clroot.ball.adapter.outbound.messaging.producer.kafka

import io.clroot.ball.adapter.outbound.messaging.producer.MessageProducerBase
import io.clroot.ball.domain.event.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Kafka 기반 메시지 프로듀서 구현
 * 도메인 이벤트를 Kafka 토픽으로 발행합니다.
 */
@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : MessageProducerBase() {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kafka에 이벤트 발행
     * 
     * @param event 발행할 도메인 이벤트
     */
    override fun doPublish(event: DomainEvent) {
        val topic = determineDestination(event)
        val key = determineKey(event)

        log.debug("Sending event to Kafka: topic={}, key={}, event={}", topic, key, event.javaClass.simpleName)

        val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(topic, key, event)

        future.whenComplete { result, ex ->
            if (ex != null) {
                log.error("Failed to send event to Kafka: topic={}, key={}, event={}", 
                    topic, key, event.javaClass.simpleName, ex)
                throw RuntimeException("Failed to send event to Kafka", ex)
            } else {
                log.debug(
                    "Event sent to Kafka: topic={}, partition={}, offset={}, key={}, event={}",
                    result.recordMetadata.topic(),
                    result.recordMetadata.partition(),
                    result.recordMetadata.offset(),
                    key,
                    event.javaClass.simpleName
                )
            }
        }
    }

    /**
     * 이벤트에서 메시지 키 결정
     * ID 필드가 있으면 해당 값을 키로 사용하고, 없으면 이벤트 클래스 이름을 사용
     * 
     * @param event 도메인 이벤트
     * @return 메시지 키
     */
    private fun determineKey(event: DomainEvent): String {
        return try {
            // ID 필드 찾기 (id, entityId, userId 등)
            val idField = event.javaClass.declaredFields
                .firstOrNull { field -> 
                    field.name == "id" || field.name.endsWith("Id") 
                }

            if (idField != null) {
                idField.isAccessible = true
                val idValue = idField.get(event)
                idValue?.toString() ?: event.javaClass.simpleName
            } else {
                // ID 필드가 없으면 이벤트 클래스 이름 사용
                event.javaClass.simpleName
            }
        } catch (e: Exception) {
            log.warn("Failed to determine key for event: {}", event.javaClass.simpleName, e)
            // 예외 발생 시 이벤트 클래스 이름을 기본값으로 사용
            event.javaClass.simpleName
        }
    }

    /**
     * 이벤트 발행 오류 처리
     * 
     * @param event 발행에 실패한 이벤트
     * @param error 발생한 예외
     */
    override fun handlePublishError(event: DomainEvent, error: Exception) {
        log.error("Error publishing event to Kafka: {}", event.javaClass.simpleName, error)
        // 여기에 재시도 로직이나 데드 레터 큐 발행 로직을 추가할 수 있음
    }
}
