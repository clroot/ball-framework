package io.clroot.ball.adapter.inbound.rest.dto

/**
 * 에러 정보 클래스
 *
 * @property code 에러 코드
 * @property message 에러 메시지
 * @property fieldErrors 필드별 에러 정보 (선택 사항)
 */
data class ErrorInfo(
    val code: String,
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap()
)