package io.clroot.ball.adapter.inbound.rest.exception

import io.clroot.ball.adapter.inbound.rest.dto.error.DebugInfo
import io.clroot.ball.adapter.inbound.rest.dto.error.ErrorResponse
import io.clroot.ball.adapter.inbound.rest.filter.RequestLoggingFilter.Companion.TRACE_ID_MDC_KEY
import io.clroot.ball.adapter.outbound.data.access.core.exception.DuplicateEntityException
import io.clroot.ball.adapter.outbound.data.access.core.exception.EntityNotFoundException
import io.clroot.ball.adapter.outbound.data.access.core.exception.PersistenceException
import io.clroot.ball.domain.exception.DomainException
import io.clroot.ball.domain.exception.ErrorType
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler(
    private val environment: Environment,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        e: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument exception: ${e.message}")
        val errorResponse =
            ErrorResponse(
                code = ErrorCodes.VALIDATION_FAILED,
                message = e.message ?: "입력값이 올바르지 않습니다",
                traceId = getTraceId(),
                debug = createDebugInfo(e, request),
            )

        return ResponseEntity
            .badRequest()
            .body(errorResponse)
    }

    /**
     * 도메인 예외 처리
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        e: DomainException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Domain exception: ${e.message}")

        val (code, status) = determineErrorCodeAndStatus(e)

        val errorResponse =
            ErrorResponse(
                code = code,
                message = e.message ?: "도메인 규칙 위반",
                traceId = getTraceId(),
                debug = createDebugInfo(e, request),
            )

        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * ErrorType을 HTTP 상태 코드로 매핑
     */
    private fun determineErrorCodeAndStatus(e: DomainException): Pair<String, HttpStatus> =
        when (e.errorType) {
            ErrorType.BAD_INPUT -> ErrorCodes.VALIDATION_FAILED to HttpStatus.BAD_REQUEST
            ErrorType.UNAUTHORIZED -> ErrorCodes.AUTHENTICATION_FAILED to HttpStatus.UNAUTHORIZED
            ErrorType.FORBIDDEN -> ErrorCodes.AUTHENTICATION_FAILED to HttpStatus.FORBIDDEN
            ErrorType.NOT_FOUND -> ErrorCodes.NOT_FOUND to HttpStatus.NOT_FOUND
            ErrorType.CONFLICT -> ErrorCodes.DUPLICATE_ENTITY to HttpStatus.CONFLICT
            ErrorType.UNPROCESSABLE -> ErrorCodes.BUSINESS_RULE_VIOLATION to HttpStatus.UNPROCESSABLE_ENTITY
            ErrorType.PRECONDITION_FAILED -> ErrorCodes.PRECONDITION_FAILED to HttpStatus.PRECONDITION_FAILED
            ErrorType.GONE -> ErrorCodes.RESOURCE_GONE to HttpStatus.GONE
            ErrorType.EXTERNAL_ERROR -> ErrorCodes.EXTERNAL_SYSTEM_ERROR to HttpStatus.INTERNAL_SERVER_ERROR
            ErrorType.EXTERNAL_TIMEOUT -> ErrorCodes.EXTERNAL_TIMEOUT to HttpStatus.INTERNAL_SERVER_ERROR
        }

    /**
     * 영속성 예외 처리
     */
    @ExceptionHandler(PersistenceException::class)
    fun handlePersistenceException(
        e: PersistenceException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Persistence exception: ${e.message}", e)

        val (code, status) =
            when (e) {
                is EntityNotFoundException -> ErrorCodes.NOT_FOUND to 404
                is DuplicateEntityException -> ErrorCodes.DUPLICATE_ENTITY to 409
                else -> ErrorCodes.DATABASE_ERROR to 500
            }

        val errorResponse =
            ErrorResponse(
                code = code,
                message = e.message ?: "데이터베이스 오류",
                traceId = getTraceId(),
                debug = createDebugInfo(e, request),
            )

        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * Bean Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation exception: ${e.message}")

        val fieldErrors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "유효하지 않은 값") }

        val errorResponse =
            ErrorResponse(
                code = ErrorCodes.VALIDATION_FAILED,
                message = "요청 유효성 검사에 실패했습니다",
                traceId = getTraceId(),
                details = fieldErrors,
                debug = createDebugInfo(e, request),
            )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleResourceNotFoundException(
        e: NoResourceFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Resource not found exception: ${e.message}", e)

        val errorResponse =
            ErrorResponse(
                code = ErrorCodes.NOT_FOUND,
                message = "요청한 경로를 찾을 수 없습니다",
                traceId = getTraceId(),
                debug = createDebugInfo(e, request),
            )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse)
    }

    /**
     * 모든 예외의 최종 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception: ${e.message}", e)

        val errorResponse =
            when (e.javaClass.simpleName) {
                "AuthorizationDeniedException" ->
                    return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                            ErrorResponse(
                                code = ErrorCodes.BUSINESS_RULE_VIOLATION,
                                message = "접근할 수 없습니다",
                                traceId = getTraceId(),
                                debug = createDebugInfo(e, request),
                            ),
                        )

                else ->
                    ErrorResponse(
                        code = ErrorCodes.INTERNAL_ERROR,
                        message = "내부 서버 오류",
                        traceId = getTraceId(),
                        debug = createDebugInfo(e, request),
                    )
            }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse)
    }

    /**
     * 디버깅 정보 생성 (개발 환경에서만)
     */
    private fun createDebugInfo(
        exception: Throwable,
        request: HttpServletRequest,
    ): DebugInfo? {
        if (!isDebugMode()) return null

        return DebugInfo(
            path = request.requestURI,
            method = request.method,
            exceptionType = exception::class.simpleName,
            stackTrace = exception.stackTraceToString(),
            location = ExceptionLocationExtractor.extractLocation(exception),
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
