package io.clroot.ball.adapter.inbound.rest.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.LocalDateTime

/**
 * Global Exception Handler
 *
 * This class handles exceptions thrown throughout the application and converts them to appropriate HTTP responses.
 * 전역 예외 처리기: 애플리케이션 전체에서 발생하는 예외를 처리하고 적절한 HTTP 응답으로 변환합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Handle method argument validation exceptions
     * 메서드 인자 검증 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ValidationErrorResponse> {
        log.warn("Method argument validation exception: {}", ex.message)
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return createValidationErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors, request)
    }

    /**
     * Handle method argument type mismatch exceptions
     * 메서드 인자 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Method argument type mismatch exception: {}", ex.message)
        val message = "Type mismatch for parameter '${ex.name}': ${ex.message}"
        return createErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    /**
     * Handle all other exceptions
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        log.error("Unexpected exception: {}", ex.message, ex)
        val message = "An unexpected error occurred: ${ex.message}"
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request)
    }

    /**
     * Create a standard error response
     * 표준 에러 응답 생성
     */
    private fun createErrorResponse(
        status: HttpStatus,
        message: String?,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = extractPath(request)
        )
        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * Create a validation error response with field errors
     * 필드 에러가 포함된 검증 에러 응답 생성
     */
    private fun createValidationErrorResponse(
        status: HttpStatus,
        message: String?,
        fieldErrors: Map<String, String>,
        request: WebRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val errorResponse = ValidationErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = extractPath(request),
            errors = fieldErrors
        )
        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * Extract the path from the request
     * 요청에서 경로 추출
     */
    private fun extractPath(request: WebRequest): String {
        return request.getDescription(false).substring(4)
    }
}

/**
 * Basic error response class
 * 기본 에러 응답 클래스
 */
data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String?,
    val path: String
)

/**
 * Validation error response class with field errors
 * 필드 에러가 포함된 검증 에러 응답 클래스
 */
data class ValidationErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String?,
    val path: String,
    val errors: Map<String, String>
)
