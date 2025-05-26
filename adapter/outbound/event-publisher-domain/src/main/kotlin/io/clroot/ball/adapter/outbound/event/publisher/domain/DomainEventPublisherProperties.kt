package io.clroot.ball.adapter.outbound.event.publisher.domain

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 도메인 이벤트 발행자 설정 프로퍼티
 * 
 * 설정 프리픽스: ball.events.domain
 * 
 * 설정 예시:
 * ```yaml
 * ball:
 *   events:
 *     domain:
 *       enabled: true
 *       async: true
 *       enable-retry: false
 *       max-retry-attempts: 3
 *       retry-delay-ms: 1000
 *       enable-debug-logging: false
 *       enable-metrics: true
 *       validation:
 *         strict: true
 *         required-fields: ["id", "type", "occurredAt"]
 * ```
 */
@ConfigurationProperties(prefix = "ball.events.domain")
data class DomainEventPublisherProperties(
    /**
     * 도메인 이벤트 발행 기능 활성화 여부
     * 
     * false로 설정하면 도메인 이벤트 발행이 비활성화됩니다.
     * 테스트 환경이나 특정 상황에서 유용합니다.
     * 
     * 기본값: true
     */
    val enabled: Boolean = true,

    /**
     * 비동기 이벤트 발행 여부
     * 
     * true: 이벤트가 비동기로 발행되어 성능이 향상됩니다.
     * false: 이벤트가 동기로 발행되어 디버깅이 쉽습니다.
     * 
     * 기본값: true
     */
    val async: Boolean = true,

    /**
     * 실패한 이벤트 재시도 기능 활성화 여부
     * 
     * 활성화되면 실패한 이벤트가 재시도 설정에 따라 다시 발행됩니다.
     * 도메인 이벤트는 비즈니스 로직에 중요하므로 기본적으로 활성화를 권장합니다.
     * 
     * 기본값: true
     */
    val enableRetry: Boolean = true,

    /**
     * 최대 재시도 횟수
     * 
     * enableRetry가 true일 때만 유효합니다.
     * 
     * 기본값: 3
     */
    val maxRetryAttempts: Int = 3,

    /**
     * 재시도 간격 (밀리초)
     * 
     * 실패한 이벤트를 재시도하기 전에 대기하는 시간입니다.
     * 
     * 기본값: 1000 (1초)
     */
    val retryDelayMs: Long = 1000,

    /**
     * 이벤트 발행 타임아웃 (밀리초)
     * 
     * 이벤트 발행이 완료되기까지 기다리는 최대 시간입니다.
     * 0으로 설정하면 타임아웃이 없습니다.
     * 
     * 기본값: 5000 (5초)
     */
    val timeoutMs: Long = 5000,

    /**
     * 디버그 로깅 활성화 여부
     * 
     * 활성화되면 이벤트 발행 과정에 대한 상세한 로그가 출력됩니다.
     * 개발 환경에서는 유용하지만 운영 환경에서는 성능에 영향을 줄 수 있습니다.
     * 
     * 기본값: false
     */
    val enableDebugLogging: Boolean = false,

    /**
     * 메트릭 수집 활성화 여부
     * 
     * 활성화되면 이벤트 발행 관련 메트릭이 수집됩니다.
     * 모니터링과 운영을 위해 활성화를 권장합니다.
     * 
     * 기본값: true
     */
    val enableMetrics: Boolean = true,

    /**
     * 이벤트 유효성 검증 설정
     */
    val validation: ValidationProperties = ValidationProperties()
) {
    
    /**
     * 이벤트 유효성 검증 관련 설정
     */
    data class ValidationProperties(
        /**
         * 엄격한 유효성 검증 여부
         * 
         * true: 모든 필수 필드가 존재하고 유효한지 검증
         * false: 기본적인 검증만 수행
         * 
         * 기본값: true
         */
        val strict: Boolean = true,

        /**
         * 필수 필드 목록
         * 
         * 도메인 이벤트에서 반드시 존재해야 하는 필드들의 목록입니다.
         * 
         * 기본값: ["id", "type", "occurredAt"]
         */
        val requiredFields: List<String> = listOf("id", "type", "occurredAt"),

        /**
         * 이벤트 ID 최대 길이
         * 
         * 이벤트 ID가 너무 길면 에러가 발생합니다.
         * 
         * 기본값: 255
         */
        val maxIdLength: Int = 255,

        /**
         * 이벤트 타입 최대 길이
         * 
         * 이벤트 타입이 너무 길면 에러가 발생합니다.
         * 
         * 기본값: 100
         */
        val maxTypeLength: Int = 100
    )
}
