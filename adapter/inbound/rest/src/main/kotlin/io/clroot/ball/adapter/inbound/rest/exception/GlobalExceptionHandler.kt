package io.clroot.ball.adapter.inbound.rest.exception

import io.clroot.ball.adapter.inbound.rest.logging.RequestLoggingFilter.Companion.TRACE_ID_MDC_KEY
import io.clroot.ball.domain.exception.*
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler(
    private val environment: Environment
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    /**
     * 도메인 예외 처리
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        e: DomainException, request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Domain exception: ${e.message}", e)

        val (code, status) = when (e) {
            is ValidationException -> ErrorCodes.VALIDATION_FAILED to 400
            is BusinessRuleViolationException -> ErrorCodes.BUSINESS_RULE_VIOLATION to 400
            is SpecificationNotSatisfiedException -> ErrorCodes.VALIDATION_FAILED to 400
            is InvalidIdException -> ErrorCodes.INVALID_ID to 400
            else -> ErrorCodes.VALIDATION_FAILED to 400
        }

        val errorResponse = ErrorResponse(
            code = code,
            message = e.message ?: "Domain rule violation",
            traceId = getTraceId(),
            debug = createDebugInfo(e, request)
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * 영속성 예외 처리
     */
    @ExceptionHandler(PersistenceException::class)
    fun handlePersistenceException(
        e: PersistenceException, request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Persistence exception: ${e.message}", e)

        val (code, status) = when (e) {
            is EntityNotFoundException -> ErrorCodes.NOT_FOUND to 404
            is DuplicateEntityException -> ErrorCodes.DUPLICATE_ENTITY to 409
            else -> ErrorCodes.DATABASE_ERROR to 500
        }

        val errorResponse = ErrorResponse(
            code = code,
            message = e.message ?: "Database error",
            traceId = getTraceId(),
            debug = createDebugInfo(e, request)
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * Bean Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException, request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation exception: ${e.message}")

        val fieldErrors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }

        val errorResponse = ErrorResponse(
            code = ErrorCodes.VALIDATION_FAILED,
            message = "Request validation failed",
            traceId = getTraceId(),
            details = fieldErrors,
            debug = createDebugInfo(e, request)
        )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 모든 예외의 최종 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        e: Exception, request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception: ${e.message}", e)

        val errorResponse = ErrorResponse(
            code = ErrorCodes.INTERNAL_ERROR,
            message = "Internal server error",
            traceId = getTraceId(),
            debug = createDebugInfo(e, request)
        )

        return ResponseEntity.status(500).body(errorResponse)
    }

    /**
     * 디버깅 정보 생성 (개발 환경에서만)
     */
    private fun createDebugInfo(
        exception: Throwable,
        request: HttpServletRequest
    ): DebugInfo? {
        if (!isDebugMode()) return null

        return DebugInfo(
            path = request.requestURI,
            method = request.method,
            exceptionType = exception::class.simpleName,
            stackTrace = exception.stackTraceToString(),
            location = SimpleLocationExtractor.extractLocation(exception) // 한 줄로 끝!
        )
    }

    /**
     * 개발 환경 확인
     */
    private fun isDebugMode(): Boolean = environment.activeProfiles.any { it in listOf("dev", "local", "development") }

    /**
     * 추적 ID 생성/조회
     */
    private fun getTraceId(): String = MDC.get(TRACE_ID_MDC_KEY) ?: UUID.randomUUID().toString()
}