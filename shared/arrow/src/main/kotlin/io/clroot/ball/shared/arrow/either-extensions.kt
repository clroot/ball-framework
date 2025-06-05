package io.clroot.ball.shared.arrow

import arrow.core.Either
import arrow.core.flatMap

fun <E, A, B> Either<E, A>.flatMapIf(
    condition: (A) -> Boolean,
    ifTrue: (A) -> Either<E, B>,
    ifFalse: (A) -> Either<E, B>
): Either<E, B> = flatMap { value ->
    if (condition(value)) ifTrue(value) else ifFalse(value)
}