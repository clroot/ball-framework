package io.clroot.ball.adapter.inbound.messaging.core.retry

import io.clroot.ball.adapter.inbound.messaging.core.MessageDispatchException
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 메시지 처리 재시도 정책 정의 클래스
 *
 * @property maxRetries 최대 재시도 횟수
 * @property initialDelayMs 초기 지연 시간 (밀리초)
 * @property maxDelayMs 최대 지연 시간 (밀리초)
 * @property backoffMultiplier 백오프 승수 (지수적 백오프에 사용)
 */
data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val backoffMultiplier: Double = 2.0
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(initialDelayMs > 0) { "initialDelayMs must be positive" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be greater than or equal to initialDelayMs" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be greater than or equal to 1.0" }
    }

    /**
     * 재시도 횟수에 따른 지연 시간 계산
     *
     * @param retryCount 현재 재시도 횟수
     * @return 다음 재시도까지의 지연 시간
     */
    fun calculateDelayDuration(retryCount: Int): Duration {
        if (retryCount <= 0) return 0.milliseconds

        // 지수적 백오프 계산 (initialDelay * backoffMultiplier^(retryCount-1))
        val delay = initialDelayMs * Math.pow(backoffMultiplier, (retryCount - 1).toDouble())
        
        // maxDelayMs를 초과하지 않도록 제한
        return min(delay.toLong(), maxDelayMs).milliseconds
    }

    /**
     * 예외가 재시도 가능한지 확인
     *
     * @param throwable 발생한 예외
     * @return 재시도 가능 여부
     */
    fun isRetryable(throwable: Throwable): Boolean {
        // MessageDispatchException인 경우 retryable 속성 확인
        if (throwable is MessageDispatchException) {
            return throwable.retryable
        }
        
        // 기본적으로 대부분의 예외는 재시도 가능하다고 간주
        // 필요에 따라 특정 예외 타입에 대한 처리 추가 가능
        return true
    }

    companion object {
        /**
         * 기본 재시도 정책 인스턴스
         */
        val DEFAULT = RetryPolicy()
        
        /**
         * 재시도 없음 정책 인스턴스
         */
        val NO_RETRY = RetryPolicy(maxRetries = 0)
    }
}