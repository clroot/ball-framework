package io.clroot.ball.domain.exception

/**
 * 도메인 계층의 기본 예외
 */
abstract class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)