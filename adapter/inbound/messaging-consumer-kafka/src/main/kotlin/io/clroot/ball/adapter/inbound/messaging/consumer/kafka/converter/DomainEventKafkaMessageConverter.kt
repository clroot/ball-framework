package io.clroot.ball.adapter.inbound.messaging.consumer.kafka.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.clroot.ball.domain.event.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 도메인 이벤트 Kafka 메시지 변환기
 * 
 * Kafka 메시지(JSON)를 도메인 이벤트 객체로 변환하는 역할을 담당합니다.
 * 
 * 메시지 형식:
 * ```json
 * {
 *   "eventType": "io.clroot.ball.user.domain.event.UserCreatedEvent",
 *   "eventId": "12345678-1234-1234-1234-123456789012",
 *   "occurredAt": "2023-01-01T00:00:00Z",
 *   "eventData": {
 *     "userId": "user123",
 *     "email": "user@example.com",
 *     ...
 *   }
 * }
 * ```
 */
@Component
class DomainEventKafkaMessageConverter(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kafka 메시지를 도메인 이벤트로 변환
     * 
     * @param message Kafka 메시지 (JSON 문자열)
     * @return 도메인 이벤트 객체 또는 null (변환 실패 시)
     */
    fun convertToDomainEvent(message: String): DomainEvent? {
        return try {
            val jsonNode = objectMapper.readTree(message)
            
            // 필수 필드 검증
            val eventType = jsonNode.get("eventType")?.asText()
            if (eventType.isNullOrBlank()) {
                log.warn("Missing or empty eventType in message: {}", message)
                return null
            }

            val eventData = jsonNode.get("eventData")
            if (eventData == null || eventData.isNull) {
                log.warn("Missing eventData in message: {}", message)
                return null
            }

            // 클래스 로딩 및 변환
            val eventClass = loadEventClass(eventType)
            if (eventClass == null) {
                log.warn("Unknown event type: {}", eventType)
                return null
            }

            val domainEvent = objectMapper.treeToValue(eventData, eventClass)
            log.debug("Successfully converted Kafka message to domain event: {} (ID: {})", 
                domainEvent.type, domainEvent.id)
            
            domainEvent

        } catch (e: Exception) {
            log.error("Failed to convert Kafka message to domain event: {}", message, e)
            null
        }
    }

    /**
     * 이벤트 타입 문자열로부터 클래스를 로드
     * 
     * @param eventType 이벤트 타입 (풀 클래스명)
     * @return 도메인 이벤트 클래스 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadEventClass(eventType: String): Class<out DomainEvent>? {
        return try {
            val clazz = Class.forName(eventType)
            
            // DomainEvent 상속 여부 확인
            if (DomainEvent::class.java.isAssignableFrom(clazz)) {
                clazz as Class<out DomainEvent>
            } else {
                log.warn("Class {} is not a DomainEvent", eventType)
                null
            }
        } catch (e: ClassNotFoundException) {
            log.warn("Event class not found: {}", eventType, e)
            null
        } catch (e: Exception) {
            log.error("Failed to load event class: {}", eventType, e)
            null
        }
    }

    /**
     * 메시지 유효성 검증
     * 
     * @param message 검증할 메시지
     * @return 유효하면 true, 그렇지 않으면 false
     */
    fun isValidMessage(message: String): Boolean {
        return try {
            val jsonNode = objectMapper.readTree(message)
            
            // 필수 필드 존재 여부 확인
            val hasEventType = jsonNode.has("eventType") && !jsonNode.get("eventType").isNull
            val hasEventData = jsonNode.has("eventData") && !jsonNode.get("eventData").isNull
            
            hasEventType && hasEventData
        } catch (e: Exception) {
            log.debug("Invalid message format: {}", message, e)
            false
        }
    }

    /**
     * 이벤트 타입 추출
     * 
     * @param message Kafka 메시지
     * @return 이벤트 타입 문자열 또는 null
     */
    fun extractEventType(message: String): String? {
        return try {
            val jsonNode = objectMapper.readTree(message)
            jsonNode.get("eventType")?.asText()
        } catch (e: Exception) {
            log.debug("Failed to extract event type from message: {}", message, e)
            null
        }
    }

    /**
     * 이벤트 ID 추출
     * 
     * @param message Kafka 메시지
     * @return 이벤트 ID 문자열 또는 null
     */
    fun extractEventId(message: String): String? {
        return try {
            val jsonNode = objectMapper.readTree(message)
            
            // eventData 내부의 id 필드 확인
            val eventData = jsonNode.get("eventData")
            eventData?.get("id")?.asText()
        } catch (e: Exception) {
            log.debug("Failed to extract event ID from message: {}", message, e)
            null
        }
    }
}
