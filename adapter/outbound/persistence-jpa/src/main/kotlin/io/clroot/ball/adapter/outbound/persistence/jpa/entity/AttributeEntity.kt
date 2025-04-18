package io.clroot.ball.adapter.outbound.persistence.jpa.entity

import io.clroot.ball.domain.model.core.BinaryId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 속성 값을 저장하기 위한 JPA 엔티티
 */
@Entity
@Table(name = "entity_attributes")
data class AttributeEntity(
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    val id: BinaryId = BinaryId.new(),

    @Column(name = "entity_id", nullable = false)
    val entityId: String,

    @Column(name = "entity_type", nullable = false)
    val entityType: String,

    @Column(name = "attr_key", nullable = false)
    val attrKey: String,

    @Column(name = "attr_value", nullable = false, columnDefinition = "TEXT")
    val attrValue: String,

    @Column(name = "value_type", nullable = false)
    val valueType: String
)
