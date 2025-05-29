package io.clroot.ball.domain.model.core

import java.time.Instant

abstract class EntityBase<ID : Any>(
    override val id: ID,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null,
    val version: Long = 0,
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
