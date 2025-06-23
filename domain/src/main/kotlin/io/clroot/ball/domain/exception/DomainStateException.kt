package io.clroot.ball.domain.exception

import io.clroot.ball.domain.model.EntityBase

class DomainStateException(
    message: String,
    val entityType: String? = null,
    val entityId: String? = null,
    cause: Throwable? = null,
) : DomainException(message, cause) {
    companion object {
        fun <T : EntityBase<*>> entityNotFound(
            entity: T,
            id: String,
        ) = DomainStateException("${entity::class.simpleName} not found: $id", entity::class.simpleName, id)

        fun <T : EntityBase<*>> entityAlreadyExists(
            entity: T,
            id: String,
        ) = DomainStateException("${entity::class.simpleName} already exists: $id", entity::class.simpleName, id)

        fun <T : EntityBase<*>> invalidState(
            entity: T,
            currentState: String,
            expectedState: String,
        ) = DomainStateException(
            "Invalid state transition: $currentState -> $expectedState",
            entity::class.simpleName,
            null,
        )
    }
}
