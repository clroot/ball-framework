package io.clroot.ball.domain.model.policy

import arrow.core.Either

/**
 * OR 정책
 * 두 정책 중 하나라도 만족하면 성공
 */
class OrPolicy<T, E>(
    private val left: Policy<T, E>,
    private val right: Policy<T, E>
) : Policy<T, E> {
    override fun validate(target: T): Either<E, Unit> {
        return when (val leftResult = left.validate(target)) {
            is Either.Right -> leftResult
            is Either.Left -> right.validate(target)
        }
    }
}