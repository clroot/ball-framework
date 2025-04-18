package io.clroot.ball.adapter.outbound.persistence.jpa.repository

import io.clroot.ball.adapter.outbound.persistence.jpa.entity.AttributeEntity
import io.clroot.ball.domain.model.core.BinaryId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AttributeJpaRepositoryTest : StringSpec({

    "repository should find all attributes for an entity" {
        // Given
        val repository = mockk<AttributeJpaRepository>()
        val entityId = "entity1"
        val entityType = "TestEntity"
        val attributes = listOf(
            AttributeEntity(
                id = BinaryId.new(),
                entityId = entityId,
                entityType = entityType,
                attrKey = "key1",
                attrValue = "value1",
                valueType = "java.lang.String"
            ),
            AttributeEntity(
                id = BinaryId.new(),
                entityId = entityId,
                entityType = entityType,
                attrKey = "key2",
                attrValue = "42",
                valueType = "java.lang.Integer"
            )
        )

        every { repository.findAllByEntityIdAndEntityType(entityId, entityType) } returns attributes

        // When
        val result = repository.findAllByEntityIdAndEntityType(entityId, entityType)

        // Then
        result shouldBe attributes
        result.size shouldBe 2
        result.map { it.attrKey } shouldContainExactlyInAnyOrder listOf("key1", "key2")
        verify { repository.findAllByEntityIdAndEntityType(entityId, entityType) }
    }

    "repository should find attribute by entity ID, type, and key" {
        // Given
        val repository = mockk<AttributeJpaRepository>()
        val entityId = "entity1"
        val entityType = "TestEntity"
        val attrKey = "key1"
        val attribute = AttributeEntity(
            id = BinaryId.new(),
            entityId = entityId,
            entityType = entityType,
            attrKey = attrKey,
            attrValue = "value1",
            valueType = "java.lang.String"
        )

        every { repository.findByEntityIdAndEntityTypeAndAttrKey(entityId, entityType, attrKey) } returns attribute

        // When
        val result = repository.findByEntityIdAndEntityTypeAndAttrKey(entityId, entityType, attrKey)

        // Then
        result shouldBe attribute
        verify { repository.findByEntityIdAndEntityTypeAndAttrKey(entityId, entityType, attrKey) }
    }

    "repository should delete all attributes for an entity" {
        // Given
        val repository = mockk<AttributeJpaRepository>()
        val entityId = "entity1"
        val entityType = "TestEntity"

        every { repository.deleteAllByEntityIdAndEntityType(entityId, entityType) } returns Unit

        // When
        repository.deleteAllByEntityIdAndEntityType(entityId, entityType)

        // Then
        verify { repository.deleteAllByEntityIdAndEntityType(entityId, entityType) }
    }

    "repository should find entity IDs by type, key, and value" {
        // Given
        val repository = mockk<AttributeJpaRepository>()
        val entityType = "TestEntity"
        val attrKey = "status"
        val attrValue = "active"
        val entityIds = listOf("entity1", "entity2")

        every { repository.findEntityIdsByTypeAndKeyAndValue(entityType, attrKey, attrValue) } returns entityIds

        // When
        val result = repository.findEntityIdsByTypeAndKeyAndValue(entityType, attrKey, attrValue)

        // Then
        result shouldBe entityIds
        verify { repository.findEntityIdsByTypeAndKeyAndValue(entityType, attrKey, attrValue) }
    }
})
