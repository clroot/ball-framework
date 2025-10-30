package io.clroot.ball.adapter.inbound.rest.support

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 디버깅 정보 DTO
 *
 * 개발 환경에서만 포함되는 에러 디버깅 정보입니다.
 * 운영 환경에서는 보안상 제외됩니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DebugInfo(
    /**
     * 요청 경로
     */
    val path: String? = null,
    /**
     * HTTP 메서드
     */
    val method: String? = null,
    /**
     * 예외 타입
     */
    val exceptionType: String? = null,
    /**
     * 스택 트레이스 (개발용)
     */
    val stackTrace: String? = null,
    /**
     * 예외 발생 위치 (요약)
     */
    val location: String? = null,
)
