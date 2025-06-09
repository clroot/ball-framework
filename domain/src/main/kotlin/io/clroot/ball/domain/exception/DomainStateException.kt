package io.clroot.ball.domain.exception

import io.clroot.ball.domain.model.EntityBase
import kotlin.reflect.KClass

class DomainStateException(
    message: String,
    val entityType: String? = null,
    val entityId: String? = null,
    cause: Throwable? = null
) : DomainException(message, cause) {

    companion object {
        fun entityNotFound(entityType: KClass<EntityBase<*>>, id: String) =
            DomainStateException("${entityType.simpleName} not found: $id", entityType.simpleName, id)

        fun entityAlreadyExists(entityType: KClass<EntityBase<*>>, id: String) =
            DomainStateException("${entityType.simpleName} already exists: $id", entityType.simpleName, id)

        fun invalidState(entityType: KClass<EntityBase<*>>, currentState: String, expectedState: String) =
            DomainStateException("Invalid state transition: $currentState -> $expectedState", entityType.simpleName, null)
    }
}