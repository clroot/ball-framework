package io.clroot.ball.domain.model

import java.time.LocalDateTime

abstract class EntityBase<ID : Any>(
    open val id: ID,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EntityBase<*>
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "${javaClass.simpleName}(id=$id)"
}
