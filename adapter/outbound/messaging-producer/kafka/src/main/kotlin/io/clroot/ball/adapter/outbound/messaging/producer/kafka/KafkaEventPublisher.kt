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
 */
@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : MessageProducerBase() {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kafka에 이벤트 발행
     */
    override fun doPublish(event: DomainEvent) {
        val topic = determineDestination(event)
        val key = determineKey(event)

        val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(topic, key, event)

        future.whenComplete { result, ex ->
            if (ex != null) {
                log.error("Failed to send event to Kafka: {}", event.javaClass.simpleName, ex)
            } else {
                log.debug(
                    "Event sent to Kafka: topic={}, partition={}, offset={}, event={}",
                    result.recordMetadata.topic(),
                    result.recordMetadata.partition(),
                    result.recordMetadata.offset(),
                    event.javaClass.simpleName
                )
            }
        }
    }

    /**
     * 이벤트에서 메시지 키 결정
     */
    private fun determineKey(event: DomainEvent): String {
        return try {
            val idField = event.javaClass.declaredFields
                .firstOrNull { it.name == "id" || it.name.endsWith("Id") }

            if (idField != null) {
                idField.isAccessible = true
                val idValue = idField.get(event)
                idValue?.toString() ?: event.javaClass.simpleName
            } else {
                event.javaClass.simpleName
            }
        } catch (e: Exception) {
            log.warn("Failed to determine key for event: {}", event.javaClass.simpleName, e)
            event.javaClass.simpleName
        }
    }

}