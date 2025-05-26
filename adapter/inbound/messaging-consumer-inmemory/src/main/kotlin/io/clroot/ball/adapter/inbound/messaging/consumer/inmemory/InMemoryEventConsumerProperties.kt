package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.EventConsumerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * InMemory Event Consumer configuration properties.
 * 
 * Core 모듈의 공통 설정을 확장하여 InMemory 전용 설정을 추가합니다.
 * 
 * Configuration prefix: ball.event.consumer.inmemory
 * 
 * Example configuration:
 * ```yaml
 * ball:
 *   event:
 *     consumer:
 *       inmemory:
 *         enabled: true
 *         # 공통 설정들 (core에서 상속)
 *         async: true
 *         parallel: true
 *         maxConcurrency: 10
 *         timeoutMs: 5000
 *         enableRetry: true
 *         maxRetryAttempts: 3
 *         retryDelayMs: 1000
 *         # InMemory 전용 설정들
 *         enableDebugLogging: false
 *         useApplicationEventPublisher: true
 * ```
 */
@ConfigurationProperties(prefix = "ball.event.consumer.inmemory")
data class InMemoryEventConsumerProperties(
    /**
     * InMemory 컨슈머 활성화 여부
     * 기본값: true
     */
    override val enabled: Boolean = true,

    /**
     * 비동기 이벤트 처리 여부  
     * 기본값: true
     */
    override val async: Boolean = true,

    /**
     * 핸들러 병렬 실행 여부
     * 기본값: true
     */
    override val parallel: Boolean = true,

    /**
     * 최대 동시 실행 수
     * 기본값: 10
     */
    override val maxConcurrency: Int = 10,

    /**
     * 핸들러 실행 타임아웃 (밀리초)
     * 기본값: 30000 (30초)
     */
    override val timeoutMs: Long = 5000,

    /**
     * 재시도 활성화 여부
     * 기본값: true
     */
    override val enableRetry: Boolean = true,

    /**
     * 최대 재시도 횟수
     * 기본값: 3
     */
    override val maxRetryAttempts: Int = 3,

    /**
     * 재시도 지연 시간 (밀리초)
     * 기본값: 1000 (1초)
     */
    override val retryDelayMs: Long = 1000,

    /**
     * 에러 핸들링 설정 (상속)
     */
    @NestedConfigurationProperty
    override val errorHandling: io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.ErrorHandlingProperties = 
        io.clroot.ball.adapter.inbound.messaging.consumer.core.properties.ErrorHandlingProperties(),

    // ========== InMemory 전용 설정 ==========

    /**
     * 디버그 로깅 활성화 여부
     * 
     * 활성화하면 이벤트 처리에 대한 상세한 로그가 출력됩니다.
     * 디버깅에 유용하지만 프로덕션에서는 성능에 영향을 줄 수 있습니다.
     * 기본값: false
     */
    val enableDebugLogging: Boolean = false,

    /**
     * Spring ApplicationEventPublisher 사용 여부
     * 
     * true: Spring의 ApplicationEventPublisher를 통해 이벤트 발행/수신
     * false: 직접적인 이벤트 처리 (테스트 등에서 사용)
     * 기본값: true
     */
    val useApplicationEventPublisher: Boolean = true
) : EventConsumerProperties(
    enabled = enabled,
    async = async,
    parallel = parallel,
    maxConcurrency = maxConcurrency,
    timeoutMs = timeoutMs,
    enableRetry = enableRetry,
    maxRetryAttempts = maxRetryAttempts,
    retryDelayMs = retryDelayMs,
    errorHandling = errorHandling
)
