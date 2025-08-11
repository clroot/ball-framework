// application/extensions/DomainExtensions.kt
package io.clroot.ball.application.extensions

import arrow.core.Either
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.DomainException
import io.clroot.ball.domain.model.policy.Policy

/**
 * Policy를 Either로 검증
 */
fun <T> Policy<T>.validateEither(target: T): Either<ApplicationError, T> =
    try {
        validate(target)
        Either.Right(target)
    } catch (e: DomainException) {
        Either.Left(ApplicationError.DomainError(e))
    } catch (e: Exception) {
        Either.Left(ApplicationError.SystemError("Policy validation failed", e))
    }

/**
 * nullable을 ApplicationError.NotFound로 변환
 */
fun <T> T?.toEitherNotFound(errorMessage: String = "Entity not found"): Either<ApplicationError, T> =
    if (this != null) Either.Right(this) else Either.Left(ApplicationError.NotFound)

/**
 * 도메인 예외를 ApplicationError로 변환하는 헬퍼
 */
inline fun <T> catchDomainException(crossinline block: () -> T): Either<ApplicationError, T> =
    try {
        Either.Right(block())
    } catch (e: DomainException) {
        Either.Left(ApplicationError.DomainError(e))
    } catch (e: Exception) {
        Either.Left(ApplicationError.SystemError(e.message ?: "Unknown error", e))
    }
