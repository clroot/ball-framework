package io.clroot.ball.domain.model.core

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * 정책 (Policy)
 *
 * 도메인 규칙을 적용하는 정책 인터페이스
 * 객체가 특정 비즈니스 규칙을 만족하는지 검증하고 결과를 반환
 *
 * @param T 검증 대상 객체 타입
 * @param E 오류 타입
 */
interface Policy<T, E> {
    /**
     * 정책 검증
     *
     * @param target 검증 대상 객체
     * @return 성공 시 Right(Unit), 실패 시 Left(오류)
     */
    fun validate(target: T): Either<E, Unit>

    /**
     * 두 정책을 AND 연산으로 결합
     *
     * @param other 결합할 다른 정책
     * @return 두 정책을 모두 만족해야 성공하는 새로운 정책
     */
    fun and(other: Policy<T, E>): Policy<T, E> = AndPolicy(this, other)

    /**
     * 두 정책을 OR 연산으로 결합
     *
     * @param other 결합할 다른 정책
     * @return 두 정책 중 하나라도 만족하면 성공하는 새로운 정책
     */
    fun or(other: Policy<T, E>): Policy<T, E> = OrPolicy(this, other)
}

/**
 * 명세 기반 정책
 *
 * 명세 패턴을 사용하여 정책을 구현
 *
 * @param T 검증 대상 객체 타입
 * @param E 오류 타입
 * @param specification 사용할 명세
 * @param errorProvider 오류 생성 함수
 */
class SpecificationPolicy<T, E>(
    private val specification: Specification<T>,
    private val errorProvider: (T) -> E
) : Policy<T, E> {
    override fun validate(target: T): Either<E, Unit> =
        if (specification.isSatisfiedBy(target)) {
            Unit.right()
        } else {
            errorProvider(target).left()
        }
}

/**
 * AND 정책
 * 두 정책을 모두 만족해야 성공
 */
class AndPolicy<T, E>(
    private val left: Policy<T, E>,
    private val right: Policy<T, E>
) : Policy<T, E> {
    override fun validate(target: T): Either<E, Unit> {
        val leftResult = left.validate(target)
        return when (leftResult) {
            is Either.Right -> right.validate(target)
            is Either.Left -> leftResult
        }
    }
}

/**
 * OR 정책
 * 두 정책 중 하나라도 만족하면 성공
 */
class OrPolicy<T, E>(
    private val left: Policy<T, E>,
    private val right: Policy<T, E>
) : Policy<T, E> {
    override fun validate(target: T): Either<E, Unit> {
        val leftResult = left.validate(target)
        return when (leftResult) {
            is Either.Right -> leftResult
            is Either.Left -> right.validate(target)
        }
    }
}

/**
 * 도메인 오류
 */
sealed class DomainError {
    /**
     * 유효성 검증 오류
     */
    data class ValidationError(val message: String) : DomainError()

    /**
     * 비즈니스 규칙 위반 오류
     */
    data class BusinessRuleViolation(val message: String) : DomainError()

    /**
     * 권한 오류
     */
    data class AuthorizationError(val message: String) : DomainError()
}
