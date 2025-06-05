package io.clroot.ball.application.usecase

import arrow.core.Option
import org.springframework.transaction.annotation.Transactional

abstract class Query<TQuery, TResult> {
    @Transactional(readOnly = true)
    abstract fun execute(query: TQuery): Option<TResult>

    protected fun <T> T?.toOption(): Option<T> = Option.fromNullable(this)
}