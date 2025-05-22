package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 인메모리 이벤트 발행자 설정 프로퍼티
 */
@ConfigurationProperties(prefix = "ball.event.publisher.inmemory")
data class InMemoryEventPublisherProperties(
    /**
     * 비동기 처리 여부
     * true: 이벤트를 비동기로 처리 (기본값)
     * false: 이벤트를 동기로 처리
     */
    val async: Boolean = true,

    /**
     * 재시도 기능 활성화 여부
     */
    val enableRetry: Boolean = false,

    /**
     * 최대 재시도 횟수
     */
    val maxRetryAttempts: Int = 3,

    /**
     * 재시도 간격 (밀리초)
     */
    val retryDelayMs: Long = 1000,

    /**
     * 이벤트 처리 타임아웃 (밀리초)
     * 0이면 타임아웃 없음
     */
    val timeoutMs: Long = 0,

    /**
     * 디버그 로깅 활성화 여부
     */
    val enableDebugLogging: Boolean = false
)
