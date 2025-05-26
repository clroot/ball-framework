package io.clroot.ball.adapter.inbound.event.consumer.core

/**
 * 이벤트 소비자 공통 설정 프로퍼티 인터페이스
 * 
 * 모든 이벤트 소비자 구현체가 공통으로 가져야 하는 설정들을 정의합니다.
 * 각 구현체별로 이 인터페이스를 확장하여 추가 설정을 정의할 수 있습니다.
 */
interface EventConsumerProperties {
    
    /**
     * 이벤트 소비자 활성화 여부
     */
    val enabled: Boolean
    
    /**
     * 비동기 이벤트 처리 여부
     */
    val async: Boolean
    
    /**
     * 재시도 기능 활성화 여부
     */
    val enableRetry: Boolean
    
    /**
     * 최대 재시도 횟수
     */
    val maxRetryAttempts: Int
    
    /**
     * 재시도 간격 (밀리초)
     */
    val retryDelayMs: Long
    
    /**
     * 이벤트 처리 타임아웃 (밀리초)
     */
    val timeoutMs: Long
    
    /**
     * 디버그 로깅 활성화 여부
     */
    val enableDebugLogging: Boolean
    
    /**
     * 메트릭 수집 활성화 여부
     */
    val enableMetrics: Boolean
}
