package io.clroot.ball.application

import io.clroot.ball.domain.model.AggregateRoot
import org.springframework.context.ApplicationEventPublisher

fun AggregateRoot<*>.publishEvents(applicationEventPublisher: ApplicationEventPublisher) {
    this.domainEvents.forEach { applicationEventPublisher.publishEvent(it) }
    this.clearEvents()
}
