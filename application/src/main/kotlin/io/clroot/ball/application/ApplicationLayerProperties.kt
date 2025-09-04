package io.clroot.ball.application

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Application Layer 설정 속성
 */
@ConfigurationProperties(prefix = "ball.application")
data class ApplicationLayerProperties(
    /**
     * Warmup 활성화 여부
     * - true: 애플리케이션 시작 시 모든 WarmupTask 실행
     * - false: warmup 비활성화 (기본값)
     */
    val warmupEnabled: Boolean = false,

    /**
     * Warmup 타임아웃 (초)
     * 전체 warmup 프로세스의 최대 실행 시간
     */
    val warmupTimeoutSeconds: Long = 30,
)