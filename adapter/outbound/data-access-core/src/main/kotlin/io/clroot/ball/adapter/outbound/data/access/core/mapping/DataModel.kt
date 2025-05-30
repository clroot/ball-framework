package io.clroot.ball.adapter.outbound.data.access.core.mapping

import io.clroot.ball.domain.model.core.EntityBase


/**
 * Interface for JPA entity classes that can be converted to domain entities
 *
 * This interface defines the contract for converting between JPA entities and domain entities.
 * All JPA entity classes should implement this interface.
 *
 * @param E The domain entity type
 */
interface DataModel<E : EntityBase<*>> {
    /**
     * Converts this JPA entity to a domain entity
     *
     * @return The domain entity
     */
    fun toDomain(): E

    fun update(entity: E)

    companion object {
        /**
         * Creates a data model from a domain entity
         *
         * @param entity The domain entity
         * @param factory A function that creates a data model from a domain entity
         * @return The data model
         */
        fun <E : EntityBase<*>, D : DataModel<E>> fromEntity(entity: E, factory: (E) -> D): D {
            return factory(entity)
        }
    }
}