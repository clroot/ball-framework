package io.clroot.ball.application.usecase

import arrow.core.Either
import io.clroot.ball.domain.model.core.AggregateRoot
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional

abstract class UseCase<TCommand, TError, TResult>(
    protected val applicationEventPublisher: ApplicationEventPublisher
) {
    @Transactional
    abstract fun execute(command: TCommand): Either<TError, TResult>

    protected fun <T> T.right(): Either<TError, T> = Either.Right(this)

    protected fun TError.left(): Either<TError, TResult> = Either.Left(this)

    protected fun <T : AggregateRoot<*>> publishEvents(aggregate: T): T {
        aggregate.domainEvents.forEach { applicationEventPublisher.publishEvent(it) }
        aggregate.clearEvents()
        return aggregate
    }
}