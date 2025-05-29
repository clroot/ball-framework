package io.clroot.ball.domain.model.policy

import arrow.core.Either

/**
 * AND 정책
 * 두 정책을 모두 만족해야 성공
 */
class AndPolicy<T, E>(
    private val left: Policy<T, E>,
    private val right: Policy<T, E>
) : Policy<T, E> {
    override fun validate(target: T): Either<E, Unit> {
        return when (val leftResult = left.validate(target)) {
            is Either.Right -> right.validate(target)
            is Either.Left -> leftResult
        }
    }
}