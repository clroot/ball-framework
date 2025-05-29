package io.clroot.ball.domain.model.policy

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.clroot.ball.domain.model.core.specification.Specification

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