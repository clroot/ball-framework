package io.clroot.ball.domain.model.policy

import arrow.core.Either

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

