package io.clroot.ball.adapter.inbound.messaging.core

import arrow.core.Either
import arrow.core.flatMap
import io.clroot.ball.shared.core.exception.DomainError
import org.slf4j.LoggerFactory

/**
 * MessageConsumer 인터페이스의 기본 구현을 제공하는 추상 클래스
 * 메시지 유효성 검증, 변환, 에러 처리 등의 공통 로직 구현
 *
 * @param P 메시지 페이로드 타입
 * @param D 변환된 도메인 객체 타입
 */
abstract class AbstractMessageConsumer<P, D> : MessageConsumer<P> {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 메시지 수신 및 처리
     *
     * @param payload 메시지 페이로드
     * @param metadata 메시지 메타데이터
     * @return 처리 결과 (성공 또는 도메인 에러)
     */
    override suspend fun consume(payload: P, metadata: MessageMetadata): Either<DomainError, Unit> {
        log.debug("Consuming message: id={}, topic={}", metadata.messageId, getTopicName())

        return try {
            // 1. 페이로드를 도메인 객체로 변환
            toDomainObject(payload, metadata)
                // 2. 도메인 객체 처리
                .flatMap { domainObject ->
                    processDomainObject(domainObject, metadata)
                }
        } catch (e: Exception) {
            log.error("Error processing message: id={}, topic={}", metadata.messageId, getTopicName(), e)
            
            // 예외를 DomainError로 변환
            Either.Left(
                DomainError.MessagingError(
                    message = "Error processing message: ${e.message}",
                    cause = e,
                    messageId = metadata.messageId,
                    topic = getTopicName()
                )
            )
        }
    }

    /**
     * 페이로드를 도메인 객체로 변환
     *
     * @param payload 메시지 페이로드
     * @param metadata 메시지 메타데이터
     * @return 변환된 도메인 객체 또는 에러
     */
    protected abstract suspend fun toDomainObject(payload: P, metadata: MessageMetadata): Either<DomainError, D>

    /**
     * 도메인 객체 처리
     *
     * @param domainObject 변환된 도메인 객체
     * @param metadata 메시지 메타데이터
     * @return 처리 결과 (성공 또는 도메인 에러)
     */
    protected abstract suspend fun processDomainObject(domainObject: D, metadata: MessageMetadata): Either<DomainError, Unit>
}