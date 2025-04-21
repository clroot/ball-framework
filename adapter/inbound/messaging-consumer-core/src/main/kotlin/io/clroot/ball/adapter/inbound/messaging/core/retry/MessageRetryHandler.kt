package io.clroot.ball.adapter.inbound.messaging.core.retry

import arrow.core.Either
import io.clroot.ball.adapter.inbound.messaging.core.MessageConsumer
import io.clroot.ball.adapter.inbound.messaging.core.MessageDispatchException
import io.clroot.ball.adapter.inbound.messaging.core.MessageMetadata
import io.clroot.ball.shared.core.exception.DomainError
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

/**
 * 메시지 처리 재시도 핸들러
 *
 * @param P 메시지 페이로드 타입
 * @property consumer 메시지 소비자
 * @property retryPolicy 재시도 정책
 */
class MessageRetryHandler<P>(
    private val consumer: MessageConsumer<P>,
    private val retryPolicy: RetryPolicy = RetryPolicy.DEFAULT
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 재시도 로직을 적용하여 메시지 처리 실행
     *
     * @param payload 메시지 페이로드
     * @param metadata 메시지 메타데이터
     * @return 처리 결과 (성공 또는 도메인 에러)
     */
    suspend fun executeWithRetry(payload: P, metadata: MessageMetadata): Either<DomainError, Unit> {
        var retryCount = 0
        var lastError: Throwable? = null

        while (retryCount <= retryPolicy.maxRetries) {
            try {
                // 메시지 소비 시도
                return consumer.consume(payload, metadata)
            } catch (e: Exception) {
                lastError = e
                
                // 재시도 가능한 예외인지 확인
                if (!retryPolicy.isRetryable(e)) {
                    log.warn("Non-retryable exception occurred, giving up: {}", e.message)
                    break
                }
                
                // 최대 재시도 횟수 도달 시 종료
                if (retryCount >= retryPolicy.maxRetries) {
                    log.warn("Max retry attempts ({}) reached, giving up", retryPolicy.maxRetries)
                    break
                }
                
                // 다음 재시도 전 지연 시간 계산 및 대기
                val delayDuration = retryPolicy.calculateDelayDuration(retryCount + 1)
                log.info(
                    "Retry attempt {} of {} after {} ms for message {}",
                    retryCount + 1, retryPolicy.maxRetries, delayDuration.inWholeMilliseconds, metadata.messageId
                )
                
                delay(delayDuration)
                retryCount++
            }
        }

        // 모든 재시도 실패 시 에러 반환
        return Either.Left(
            DomainError.MessagingError(
                message = "Failed to process message after $retryCount retries",
                cause = lastError,
                messageId = metadata.messageId,
                topic = consumer.getTopicName()
            )
        )
    }
}