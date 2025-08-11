package io.clroot.ball.domain.exception

/**
 * 도메인 계층의 기본 예외
 */
abstract class DomainException(
    message: String,
    val errorType: ErrorType = ErrorType.UNPROCESSABLE,
    cause: Throwable? = null
) : RuntimeException(message, cause)