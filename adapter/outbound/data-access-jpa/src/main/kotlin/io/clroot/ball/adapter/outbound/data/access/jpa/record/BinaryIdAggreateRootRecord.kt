package io.clroot.ball.adapter.outbound.data.access.jpa.record

import io.clroot.ball.domain.model.core.AggregateRoot
import io.clroot.ball.domain.model.core.EntityBase
import io.clroot.ball.domain.model.vo.BinaryId
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.Instant

/**
 * Base entity class with version information for optimistic locking
 *
 * This class provides a version field for optimistic locking.
 * It is meant to be extended by JPA entity classes that need optimistic locking capabilities.
 */
@MappedSuperclass
abstract class BinaryIdAggreateRootRecord<E : EntityBase<BinaryId>>(
    id: BinaryId,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    version: Long,
) : BinaryIdEntityRecord<E>(id, createdAt, updatedAt, deletedAt) {
    /**
     * The version of this entity, used for optimistic locking
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = version
        protected set

    constructor(entity: AggregateRoot<BinaryId>, version: Long) : this(
        id = entity.id,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
        version = version,
    )
}