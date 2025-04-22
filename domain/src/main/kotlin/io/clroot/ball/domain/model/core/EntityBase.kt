package io.clroot.ball.domain.model.core

import io.clroot.ball.shared.core.model.Entity
import java.time.Instant

abstract class EntityBase<ID : Any>(
    override val id: ID,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null
) : Entity<ID> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EntityBase<*>
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "${javaClass.simpleName}(id=$id)"
}
