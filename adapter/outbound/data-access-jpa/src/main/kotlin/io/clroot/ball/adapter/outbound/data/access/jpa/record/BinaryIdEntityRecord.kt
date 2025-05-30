package io.clroot.ball.adapter.outbound.data.access.jpa.record

import io.clroot.ball.adapter.outbound.data.access.core.mapping.DataModel
import io.clroot.ball.adapter.outbound.data.access.jpa.converter.BinaryIdConverter
import io.clroot.ball.domain.model.core.EntityBase
import io.clroot.ball.domain.model.vo.BinaryId
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@MappedSuperclass
abstract class BinaryIdEntityRecord<E : EntityBase<BinaryId>>(
    id: BinaryId,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
) : DataModel<E> {
    @Id
    @Convert(converter = BinaryIdConverter::class)
    @Column(columnDefinition = "BINARY(16)")
    var id: BinaryId = id
        protected set

    @CreationTimestamp
    var createdAt: Instant = createdAt
        protected set

    @UpdateTimestamp
    var updatedAt: Instant = updatedAt
        protected set

    var deletedAt: Instant? = deletedAt
        protected set

    constructor(entity: EntityBase<BinaryId>) : this(
        id = entity.id,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
    )
}