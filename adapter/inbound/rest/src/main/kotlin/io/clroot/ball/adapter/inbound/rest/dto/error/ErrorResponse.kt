package io.clroot.ball.adapter.inbound.rest.dto.error

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * 에러 응답 DTO
 * 
 * REST API에서 발생한 모든 에러에 대한 표준 응답 형태입니다.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ErrorResponse(
    /**
     * 에러 코드 (애플리케이션 정의)
     */
    val code: String,

    /**
     * 에러 메시지
     */
    val message: String,

    /**
     * 에러 발생 시간
     */
    val timestamp: Instant = Instant.now(),

    /**
     * 추적 ID (로그 연계용)
     */
    val traceId: String? = null,

    /**
     * 상세 에러 정보 (검증 실패 시 필드별 오류 등)
     */
    val details: Map<String, Any>? = null,

    /**
     * 디버깅 정보 (개발 환경에서만)
     */
    val debug: DebugInfo? = null
)