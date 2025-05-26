package io.clroot.ball.adapter.inbound.event.consumer.domain

import io.clroot.ball.adapter.inbound.event.consumer.core.EventConsumerProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

/**
 * 도메인 이벤트 소비자 설정 프로퍼티
 * 
 * 도메인 이벤트 처리에 특화된 설정들을 정의합니다.
 * Spring ApplicationEvent 기반의 인메모리 처리에 최적화되어 있습니다.
 */
@ConfigurationProperties(prefix = "ball.events.domain.consumer")
data class DomainEventConsumerProperties @ConstructorBinding constructor(
    
    /**
     * 도메인 이벤트 소비자 활성화 여부
     * 기본값: true (도메인 이벤트는 기본적으로 활성화)
     */
    override val enabled: Boolean = true,
    
    /**
     * 비동기 이벤트 처리 여부
     * 기본값: false (도메인 이벤트는 주로 동기 처리)
     */
    override val async: Boolean = false,
    
    /**
     * 재시도 기능 활성화 여부
     * 기본값: false (인메모리 처리라 네트워크 오류 등이 적음)
     */
    override val enableRetry: Boolean = false,
    
    /**
     * 최대 재시도 횟수
     * 기본값: 3회
     */
    override val maxRetryAttempts: Int = 3,
    
    /**
     * 재시도 간격 (밀리초)
     * 기본값: 100ms (빠른 재시도)
     */
    override val retryDelayMs: Long = 100L,
    
    /**
     * 이벤트 처리 타임아웃 (밀리초)
     * 기본값: 5초 (도메인 로직은 빨라야 함)
     */
    override val timeoutMs: Long = 5_000L,
    
    /**
     * 디버그 로깅 활성화 여부
     * 기본값: false
     */
    override val enableDebugLogging: Boolean = false,
    
    /**
     * 메트릭 수집 활성화 여부
     * 기본값: true
     */
    override val enableMetrics: Boolean = true,
    
    // === 도메인 이벤트 특화 설정들 ===
    
    /**
     * Spring ApplicationEvent 기반 처리 활성화 여부
     * 기본값: true
     */
    val useSpringApplicationEvent: Boolean = true,
    
    /**
     * 트랜잭션 내에서 이벤트 처리 여부
     * 기본값: true (도메인 이벤트는 주로 트랜잭션 내 처리)
     */
    val processInTransaction: Boolean = true,
    
    /**
     * 트랜잭션 커밋 후 처리 여부
     * 기본값: false (즉시 처리)
     */
    val processAfterCommit: Boolean = false

) : EventConsumerProperties
