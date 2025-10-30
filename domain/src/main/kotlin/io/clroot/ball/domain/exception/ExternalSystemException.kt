package io.clroot.ball.domain.exception

/**
 * 외부 시스템 연동 중 발생하는 예외의 기본 클래스
 * 
 * 외부 API, 결제 게이트웨이, 메시징 시스템 등과의 연동에서
 * 발생하는 모든 예외의 부모 클래스입니다.
 */
sealed class ExternalSystemException(
    message: String,
    errorType: ErrorType,
    val systemName: String,
    val externalErrorCode: String? = null,
    errorCode: String,
    messageKey: String,
    messageArgs: Map<String, Any?> = emptyMap(),
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : DomainException(
        message = message,
        errorType = errorType,
        errorCode = errorCode,
        messageKey = messageKey,
        messageArgs = messageArgs,
        metadata =
            buildMap {
                put("systemName", systemName)
                externalErrorCode?.let { put("externalErrorCode", it) }
                putAll(metadata)
            },
        cause = cause,
    ) {

    class General(
        systemName: String,
        detailMessage: String,
        externalErrorCode: String? = null,
        cause: Throwable? = null,
    ) : ExternalSystemException(
            message = "$systemName 오류: $detailMessage",
            errorType = ErrorType.EXTERNAL_ERROR,
            systemName = systemName,
            externalErrorCode = externalErrorCode,
            errorCode = DomainErrorCodes.EXTERNAL_SYSTEM_ERROR,
            messageKey = "external_system.error",
            messageArgs = mapOf("system" to systemName),
            metadata = mapOf("detail" to detailMessage),
            cause = cause,
        )

    class Timeout(
        systemName: String,
        timeoutMs: Long? = null,
        cause: Throwable? = null,
    ) : ExternalSystemException(
            message = "$systemName 응답 시간 초과" + (timeoutMs?.let { " (${it}ms)" } ?: ""),
            errorType = ErrorType.EXTERNAL_TIMEOUT,
            systemName = systemName,
            errorCode = DomainErrorCodes.EXTERNAL_SYSTEM_TIMEOUT,
            messageKey = "external_system.timeout",
            messageArgs = buildMap {
                put("system", systemName)
                timeoutMs?.let { put("timeoutMs", it) }
            },
            metadata = timeoutMs?.let { mapOf("timeoutMs" to it) } ?: emptyMap(),
            cause = cause,
        )

    class Unavailable(
        systemName: String,
        reason: String? = null,
        cause: Throwable? = null,
    ) : ExternalSystemException(
            message = "$systemName 서비스를 사용할 수 없습니다" + (reason?.let { ": $it" } ?: ""),
            errorType = ErrorType.GONE,
            systemName = systemName,
            errorCode = DomainErrorCodes.EXTERNAL_SYSTEM_UNAVAILABLE,
            messageKey = "external_system.unavailable",
            messageArgs = mapOf("system" to systemName),
            metadata = reason?.let { mapOf("reason" to it) } ?: emptyMap(),
            cause = cause,
        )

    companion object {
        /** 외부 시스템 일반 오류 */
        fun error(system: String, message: String, code: String? = null, cause: Throwable? = null): ExternalSystemException =
            General(systemName = system, detailMessage = message, externalErrorCode = code, cause = cause)

        /** 외부 시스템 타임아웃 */
        fun timeout(system: String, timeoutMs: Long? = null, cause: Throwable? = null): ExternalSystemException =
            Timeout(systemName = system, timeoutMs = timeoutMs, cause = cause)

        /** 외부 시스템 사용 불가 */
        fun unavailable(system: String, reason: String? = null, cause: Throwable? = null): ExternalSystemException =
            Unavailable(systemName = system, reason = reason, cause = cause)
    }
}
