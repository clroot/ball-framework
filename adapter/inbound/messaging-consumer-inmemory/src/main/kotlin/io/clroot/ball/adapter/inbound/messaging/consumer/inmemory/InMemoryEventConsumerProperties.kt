package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 인메모리 이벤트 컨슈머 설정 프로퍼티
 */
@ConfigurationProperties(prefix = "ball.event.consumer.inmemory")
data class InMemoryEventConsumerProperties(
    /**
     * 컨슈머 활성화 여부
     */
    val enabled: Boolean = true,

    /**
     * 비동기 처리 여부
     * true: 이벤트를 비동기로 처리 (기본값)
     * false: 이벤트를 동기로 처리
     */
    val async: Boolean = true,

    /**
     * 병렬 처리 여부
     * 여러 핸들러가 있을 때 병렬로 실행할지 순차적으로 실행할지 결정
     */
    val parallel: Boolean = true,

    /**
     * 최대 동시 실행 수
     * 병렬 처리 시 동시에 실행할 수 있는 핸들러의 최대 개수
     */
    val maxConcurrency: Int = 10,

    /**
     * 이벤트 처리 타임아웃 (밀리초)
     * 0이면 타임아웃 없음
     */
    val timeoutMs: Long = 5000,

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
     * 디버그 로깅 활성화 여부
     */
    val enableDebugLogging: Boolean = false
)
