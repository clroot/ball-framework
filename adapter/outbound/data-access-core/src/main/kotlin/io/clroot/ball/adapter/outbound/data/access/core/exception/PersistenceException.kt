package io.clroot.ball.adapter.outbound.data.access.core.exception

import io.clroot.ball.domain.exception.DomainErrorCodes
import io.clroot.ball.domain.exception.DomainException
import io.clroot.ball.domain.exception.ErrorType

/**
 * 영속성 계층에서 발생하는 기본 예외
 *
 * Repository 인터페이스의 작업 중 발생할 수 있는 모든 영속성 관련 예외의 기본 클래스입니다.
 *
 * @since 2.0
 */
abstract class PersistenceException(
    message: String,
    errorType: ErrorType = ErrorType.EXTERNAL_ERROR,
    code: String = DomainErrorCodes.PERSISTENCE_ERROR,
    messageKey: String? = "persistence.error",
    messageArgs: Map<String, Any?> = emptyMap(),
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : DomainException(
        message = message,
        errorType = errorType,
        errorCode = code,
        messageKey = messageKey,
        messageArgs = messageArgs,
        metadata = metadata,
        cause = cause,
    )
