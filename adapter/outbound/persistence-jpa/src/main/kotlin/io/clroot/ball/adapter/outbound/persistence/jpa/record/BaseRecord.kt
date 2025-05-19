package io.clroot.ball.adapter.outbound.persistence.jpa.record

import io.clroot.ball.adapter.outbound.persistence.core.mapping.DataModel
import io.clroot.ball.domain.model.core.EntityBase
import io.clroot.ball.shared.core.model.Entity
import jakarta.persistence.MappedSuperclass
import java.time.Instant

@MappedSuperclass
abstract class BaseRecord<ID : Any, E : Entity<ID>>(
    id: ID,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
) : DataModel<E> {
    open var id: ID = id
        protected set

    var createdAt: Instant = createdAt
        protected set

    var updatedAt: Instant = updatedAt
        protected set

    var deletedAt: Instant? = deletedAt
        protected set

    constructor(entity: EntityBase<ID>) : this(
        id = entity.id,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
    )
}