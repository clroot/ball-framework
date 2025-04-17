package io.clroot.ball.shared.arrow

import arrow.core.Either
import arrow.core.Option

fun <T, E> Option<T>.toEither(error: E): Either<E, T> =
    fold({ Either.Left(error) }, { Either.Right(it) })

@Suppress("UnusedReceiverParameter")
fun <T> Nothing?.toOption(): Option<T> = Option.fromNullable(null)