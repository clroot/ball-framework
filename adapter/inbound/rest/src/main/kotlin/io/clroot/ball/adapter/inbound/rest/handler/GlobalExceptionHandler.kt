package io.clroot.ball.adapter.inbound.rest.handler

import io.clroot.ball.adapter.inbound.rest.dto.response.ErrorResponse
import io.clroot.ball.adapter.inbound.rest.filter.RequestLoggingFilter.Companion.TRACE_ID_MDC_KEY
import io.clroot.ball.adapter.inbound.rest.support.DebugInfo
import io.clroot.ball.adapter.inbound.rest.support.ErrorCodes
import io.clroot.ball.adapter.inbound.rest.support.ExceptionLocationExtractor
import io.clroot.ball.adapter.outbound.data.access.core.exception.PersistenceException
import io.clroot.ball.domain.exception.DomainErrorCodes
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

        val errorResponse =
            ErrorResponse(
                code = resolveErrorCode(e.errorCode, e.errorType),
                message = e.message ?: "도메인 규칙 위반",
                traceId = getTraceId(),
                messageKey = e.messageKey,
                arguments = e.messageArgs.takeIf { it.isNotEmpty() },
                metadata = e.metadata.takeIf { it.isNotEmpty() },
                debug = createDebugInfo(e, request),
            )

        return ResponseEntity.status(determineHttpStatus(e.errorType)).body(errorResponse)
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

        val errorResponse =
            ErrorResponse(
                code = resolveErrorCode(e.errorCode, e.errorType),
                message = e.message ?: "데이터베이스 오류",
                traceId = getTraceId(),
                messageKey = e.messageKey,
                arguments = e.messageArgs.takeIf { it.isNotEmpty() },
                metadata = e.metadata.takeIf { it.isNotEmpty() },
                debug = createDebugInfo(e, request),
            )

        return ResponseEntity.status(determineHttpStatus(e.errorType)).body(errorResponse)
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
        if (!isDebugMode()) {
            logger.error("Unexpected exception: ${e.message}", e)
        } else {
            logger.error("Unexpected exception: ${e.message}")
        }

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

    private fun resolveErrorCode(
        errorCode: String,
        errorType: ErrorType,
    ): String = errorCode.ifBlank { defaultErrorCode(errorType) }

    private fun defaultErrorCode(errorType: ErrorType): String =
        when (errorType) {
            ErrorType.BAD_INPUT -> DomainErrorCodes.BAD_INPUT
            ErrorType.UNAUTHORIZED -> DomainErrorCodes.UNAUTHORIZED
            ErrorType.FORBIDDEN -> DomainErrorCodes.FORBIDDEN
            ErrorType.NOT_FOUND -> DomainErrorCodes.NOT_FOUND
            ErrorType.CONFLICT -> DomainErrorCodes.CONFLICT
            ErrorType.UNPROCESSABLE -> DomainErrorCodes.UNPROCESSABLE
            ErrorType.PRECONDITION_FAILED -> DomainErrorCodes.PRECONDITION_FAILED
            ErrorType.GONE -> DomainErrorCodes.RESOURCE_GONE
            ErrorType.EXTERNAL_ERROR -> DomainErrorCodes.EXTERNAL_SYSTEM_ERROR
            ErrorType.EXTERNAL_TIMEOUT -> DomainErrorCodes.EXTERNAL_SYSTEM_TIMEOUT
            ErrorType.INTERNAL_ERROR -> DomainErrorCodes.UNKNOW_ERROR
        }

    private fun determineHttpStatus(errorType: ErrorType): HttpStatus =
        when (errorType) {
            ErrorType.BAD_INPUT -> HttpStatus.BAD_REQUEST
            ErrorType.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
            ErrorType.FORBIDDEN -> HttpStatus.FORBIDDEN
            ErrorType.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorType.CONFLICT -> HttpStatus.CONFLICT
            ErrorType.UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_ENTITY
            ErrorType.PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED
            ErrorType.GONE -> HttpStatus.GONE
            ErrorType.EXTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
            ErrorType.EXTERNAL_TIMEOUT -> HttpStatus.INTERNAL_SERVER_ERROR
            ErrorType.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
        }
}
