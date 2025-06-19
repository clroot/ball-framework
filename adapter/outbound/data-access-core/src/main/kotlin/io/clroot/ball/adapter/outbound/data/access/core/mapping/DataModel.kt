package io.clroot.ball.adapter.outbound.data.access.core.mapping

import io.clroot.ball.domain.model.EntityBase

/**
 * Interface for JPA entity classes that can be converted to domain entities
 *
 * This interface defines the contract for converting between JPA entities and domain entities.
 * All JPA entity classes should implement this interface.
 *
 * @param E The domain entity type
 */
interface DataModel<E : EntityBase<*>> {
    fun update(entity: E)
}
