package io.clroot.ball.domain.exception

/**
 * 도메인 계층의 기본 예외
 */
abstract class DomainException(
    message: String,
    val errorType: ErrorType = ErrorType.UNPROCESSABLE,
    val errorCode: String = DomainErrorCodes.DOMAIN_ERROR,
    val messageKey: String? = null,
    val messageArgs: Map<String, Any?> = emptyMap(),
    val metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    init {
        require(errorCode.isNotBlank()) { "errorCode must not be blank" }
    }
}
