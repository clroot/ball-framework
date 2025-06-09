// application/extensions/DomainExtensions.kt
package io.clroot.ball.application.extensions

import arrow.core.Either
import arrow.core.flatMap
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.DomainException
import io.clroot.ball.domain.exception.DomainValidationException
import io.clroot.ball.domain.model.policy.Policy
import io.clroot.ball.domain.model.specification.Specification

/**
 * 도메인 계층의 Specification을 Application 계층의 Either로 변환하는 확장 함수들
 *
 * 도메인은 순수하게 유지하면서, Application에서 함수형 스타일로 활용
 */

/**
 * Specification을 Either로 검증 (커스텀 에러)
 */
fun <T, E> Specification<T>.validateEither(target: T, error: E): Either<E, T> =
    if (isSatisfiedBy(target)) Either.Right(target) else Either.Left(error)

/**
 * Specification을 Either로 검증 (ApplicationError 사용)
 */
fun <T> Specification<T>.validateEither(
    target: T,
    errorMessage: String = "Specification not satisfied"
): Either<ApplicationError, T> =
    if (isSatisfiedBy(target)) {
        Either.Right(target)
    } else {
        Either.Left(ApplicationError.SystemError(errorMessage, null))
    }

/**
 * Specification을 Either로 검증 (DomainError로 변환)
 */
fun <T> Specification<T>.validateEitherWithDomain(
    target: T,
    errorMessage: String = "Specification not satisfied"
): Either<ApplicationError, T> =
    if (isSatisfiedBy(target)) {
        Either.Right(target)
    } else {
        val domainException = DomainValidationException(errorMessage)
        Either.Left(ApplicationError.DomainError(domainException))
    }

/**
 * 여러 Specification을 순차적으로 검증 (첫 번째 실패에서 중단)
 */
fun <T> List<Specification<T>>.validateAllEither(
    target: T,
    errorMessage: String = "One or more specifications failed"
): Either<ApplicationError, T> {
    for (spec in this) {
        if (!spec.isSatisfiedBy(target)) {
            val domainException = DomainValidationException(errorMessage)
            return Either.Left(ApplicationError.DomainError(domainException))
        }
    }
    return Either.Right(target)
}

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
inline fun <T> catchDomainException(
    crossinline block: () -> T
): Either<ApplicationError, T> = try {
    Either.Right(block())
} catch (e: DomainException) {
    Either.Left(ApplicationError.DomainError(e))
} catch (e: Exception) {
    Either.Left(ApplicationError.SystemError(e.message ?: "Unknown error", e))
}

/**
 * 조건부 검증 - 조건이 true일 때만 Specification 실행
 */
fun <T> Specification<T>.validateEitherIf(
    target: T,
    condition: Boolean,
    errorMessage: String = "Conditional specification failed"
): Either<ApplicationError, T> =
    if (!condition) {
        Either.Right(target)
    } else {
        validateEitherWithDomain(target, errorMessage)
    }

/**
 * Specification 체이닝을 위한 확장 함수
 */
fun <T> Either<ApplicationError, T>.andThenValidate(
    specification: Specification<T>,
    errorMessage: String = "Chained specification failed"
): Either<ApplicationError, T> = flatMap { target ->
    specification.validateEitherWithDomain(target, errorMessage)
}