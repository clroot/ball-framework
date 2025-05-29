package io.clroot.ball.adapter.inbound.rest.dto

import java.time.Instant

/**
 * API 응답 래퍼 클래스
 *
 * 이 클래스는 모든 API 응답에 대한 일관된 형식을 제공합니다.
 *
 * @param T 응답 데이터 타입
 * @property timestamp 응답 생성 시간
 * @property success 성공 여부
 * @property data 응답 데이터 (성공 시)
 * @property error 에러 정보 (실패 시)
 */
data class ApiResponse<T>(
    val timestamp: Instant = Instant.now(),
    val success: Boolean,
    val data: T? = null,
    val error: ErrorInfo? = null
) {
    companion object {
        /**
         * 성공 응답 생성
         *
         * @param data 응답 데이터
         * @return 성공 응답 객체
         */
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data
            )
        }

        /**
         * 에러 응답 생성
         *
         * @param code 에러 코드
         * @param message 에러 메시지
         * @return 에러 응답 객체
         */
        fun <T> error(code: String, message: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorInfo(code, message)
            )
        }

        /**
         * 에러 응답 생성 (상세 필드 에러 포함)
         *
         * @param code 에러 코드
         * @param message 에러 메시지
         * @param fieldErrors 필드별 에러 정보
         * @return 에러 응답 객체
         */
        fun <T> error(code: String, message: String, fieldErrors: Map<String, String>): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorInfo(code, message, fieldErrors)
            )
        }
    }
}

