package io.clroot.ball.adapter.inbound.messaging.kafka.consumer

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.clroot.ball.adapter.inbound.messaging.core.MessageMetadata
import io.clroot.ball.adapter.inbound.messaging.kafka.config.KafkaConsumerProperties
import io.clroot.ball.adapter.inbound.messaging.kafka.exception.KafkaProcessingException
import io.clroot.ball.shared.core.exception.DomainError
import org.slf4j.LoggerFactory

/**
 * JSON 메시지를 처리하는 Kafka 소비자 추상 클래스
 * String 형태의 JSON을 지정된 클래스로 역직렬화하여 처리
 *
 * @param D 변환된 도메인 객체 타입
 * @property objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
 * @property properties Kafka 소비자 설정
 */
abstract class AbstractJsonKafkaConsumer<P, D>(
    private val objectMapper: ObjectMapper,
    private val properties: KafkaConsumerProperties
) : AbstractKafkaConsumer<String, D>() {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 페이로드 클래스 타입
     * JSON 역직렬화에 사용됨
     */
    abstract val payloadClass: Class<P>

    /**
     * 소비자 그룹 ID 반환
     * 기본적으로 설정의 접두사 + 토픽 이름 사용
     *
     * @return 소비자 그룹 ID
     */
    override fun getConsumerGroupId(): String {
        return "${properties.consumerGroupIdPrefix}-${getTopicName()}"
    }

    /**
     * JSON 문자열을 도메인 객체로 변환
     *
     * @param payload JSON 문자열
     * @param metadata 메시지 메타데이터
     * @return 변환된 도메인 객체 또는 에러
     */
    override suspend fun toDomainObject(payload: String, metadata: MessageMetadata): Either<DomainError, D> {
        return try {
            // JSON 문자열을 페이로드 클래스로 역직렬화
            val deserializedPayload = objectMapper.readValue(payload, payloadClass)
            
            // 역직렬화된 페이로드를 도메인 객체로 매핑
            mapToDomainObject(deserializedPayload, metadata)
        } catch (e: Exception) {
            log.error("Failed to deserialize JSON payload: {}", e.message)
            
            DomainError.MessagingError(
                message = "Failed to deserialize JSON payload: ${e.message}",
                cause = e,
                messageId = metadata.messageId,
                topic = getTopicName()
            ).left()
        }
    }

    /**
     * 역직렬화된 페이로드를 도메인 객체로 매핑
     * 하위 클래스에서 구현해야 함
     *
     * @param deserializedPayload 역직렬화된 페이로드
     * @param metadata 메시지 메타데이터
     * @return 매핑된 도메인 객체 또는 에러
     */
    abstract suspend fun mapToDomainObject(deserializedPayload: P, metadata: MessageMetadata): Either<DomainError, D>
}