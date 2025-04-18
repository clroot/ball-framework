package io.clroot.ball.adapter.inbound.rest.dto

import java.time.LocalDateTime

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
    val timestamp: LocalDateTime = LocalDateTime.now(),
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

/**
 * 페이징 응답 클래스
 * 
 * 이 클래스는 페이징된 데이터에 대한 일관된 형식을 제공합니다.
 * 
 * @param T 페이지 항목 타입
 * @property content 페이지 내용
 * @property page 현재 페이지 번호 (0부터 시작)
 * @property size 페이지 크기
 * @property totalElements 전체 항목 수
 * @property totalPages 전체 페이지 수
 * @property first 첫 페이지 여부
 * @property last 마지막 페이지 여부
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
) {
    companion object {
        /**
         * 페이지 정보와 컨텐츠로부터 PageResponse 생성
         * 
         * @param content 페이지 내용
         * @param page 현재 페이지 번호 (0부터 시작)
         * @param size 페이지 크기
         * @param totalElements 전체 항목 수
         * @param totalPages 전체 페이지 수
         * @return PageResponse 객체
         */
        fun <T> of(
            content: List<T>,
            page: Int,
            size: Int,
            totalElements: Long,
            totalPages: Int
        ): PageResponse<T> {
            return PageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
                first = page == 0,
                last = page >= totalPages - 1
            )
        }

        /**
         * 컬렉션으로부터 PageResponse 생성
         * 
         * @param items 항목 컬렉션
         * @param page 페이지 번호 (0부터 시작)
         * @param size 페이지 크기
         * @return PageResponse 객체
         */
        fun <T> from(items: Collection<T>, page: Int, size: Int): PageResponse<T> {
            val totalElements = items.size.toLong()
            val totalPages = if (size > 0) (totalElements + size - 1) / size else 0
            val pageItems = items.drop(page * size).take(size)

            return PageResponse(
                content = pageItems,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages.toInt(),
                first = page == 0,
                last = page >= totalPages - 1
            )
        }
    }
}
