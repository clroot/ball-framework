package io.clroot.ball.application.usecase

import arrow.core.Either
import arrow.core.raise.either
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.ExternalSystemException
import io.clroot.ball.domain.model.AggregateRoot
import io.clroot.ball.domain.slf4j
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional

abstract class UseCase<TCommand, TResult>(
    protected val applicationEventPublisher: ApplicationEventPublisher,
) {
    val log = slf4j()

    @Transactional
    open fun execute(command: TCommand): Either<ApplicationError, TResult> =
        either {
            try {
                val result = executeInternal(command)
                result
            } catch (e: ExternalSystemException) {
                log.warn("External system error: ${e.message}", e)
                raise(ApplicationError.ExternalSystemError(e))
            } catch (e: io.clroot.ball.domain.exception.DomainException) {
                raise(ApplicationError.DomainError(e))
            } catch (e: Exception) {
                log.error("Unhandled exception", e)
                raise(ApplicationError.SystemError(e.message ?: "Unknown error", e))
            }
        }

    protected abstract fun executeInternal(command: TCommand): TResult

    protected fun <T : AggregateRoot<*>> T.publishEvents() {
        this.domainEvents.forEach { applicationEventPublisher.publishEvent(it) }
        this.clearEvents()
    }
}
