package io.clroot.ball.adapter.inbound.messaging.consumer.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 이벤트 컨슈머 공통 설정 프로퍼티
 * 
 * 모든 messaging-consumer 모듈에서 공통으로 사용되는 기본 설정입니다.
 * 
 * 설정 예시:
 * ```yaml
 * ball:
 *   event:
 *     consumer:
 *       enabled: true
 *       async: true
 *       parallel: true
 *       maxConcurrency: 10
 *       timeoutMs: 30000
 *       enableRetry: true
 *       maxRetryAttempts: 3
 *       retryDelayMs: 1000
 * ```
 */
@ConfigurationProperties(prefix = "ball.event.consumer")
data class EventConsumerProperties(
    /**
     * 이벤트 컨슈머 활성화 여부
     * 기본값: true
     */
    val enabled: Boolean = true,

    /**
     * 비동기 이벤트 처리 여부
     * - true: 코루틴 기반 비동기 처리
     * - false: 동기 처리 (runBlocking 사용)
     * 기본값: true
     */
    val async: Boolean = true,

    /**
     * 핸들러 병렬 실행 여부
     * - true: 같은 이벤트에 대한 여러 핸들러를 병렬로 실행
     * - false: 핸들러들을 순차적으로 실행
     * 기본값: true
     */
    val parallel: Boolean = true,

    /**
     * 최대 동시 실행 수
     * 병렬 처리 시 동시에 실행할 수 있는 최대 핸들러 수를 제한합니다.
     * 기본값: 10
     */
    val maxConcurrency: Int = 10,

    /**
     * 핸들러 실행 타임아웃 (밀리초)
     * 0이면 타임아웃 없음
     * 기본값: 30000 (30초)
     */
    val timeoutMs: Long = 30000,

    /**
     * 재시도 활성화 여부
     * 핸들러 실행 실패 시 재시도를 수행할지 여부
     * 기본값: true
     */
    val enableRetry: Boolean = true,

    /**
     * 최대 재시도 횟수
     * 기본값: 3
     */
    val maxRetryAttempts: Int = 3,

    /**
     * 재시도 지연 시간 (밀리초)
     * 재시도 시 대기할 시간
     * 기본값: 1000 (1초)
     */
    val retryDelayMs: Long = 1000,

    /**
     * 에러 핸들링 설정
     */
    val errorHandling: ErrorHandlingProperties = ErrorHandlingProperties()
)

/**
 * 에러 핸들링 관련 설정
 */
data class ErrorHandlingProperties(
    /**
     * Dead Letter Queue 활성화 여부
     * 재시도가 모두 실패한 이벤트를 별도로 저장할지 여부
     * 기본값: false
     */
    val enableDeadLetterQueue: Boolean = false,

    /**
     * 에러 로그 레벨
     * ERROR, WARN, INFO, DEBUG 중 선택
     * 기본값: ERROR
     */
    val logLevel: String = "ERROR",

    /**
     * 에러 알림 활성화 여부
     * 에러 발생 시 외부 시스템에 알림을 보낼지 여부
     * 기본값: false
     */
    val enableNotification: Boolean = false
)
