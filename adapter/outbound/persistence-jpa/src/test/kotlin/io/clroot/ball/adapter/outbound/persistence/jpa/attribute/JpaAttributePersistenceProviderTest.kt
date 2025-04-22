package io.clroot.ball.adapter.outbound.persistence.jpa.attribute

import arrow.core.some
import io.clroot.ball.adapter.outbound.persistence.jpa.entity.AttributeEntity
import io.clroot.ball.adapter.outbound.persistence.jpa.repository.AttributeJpaRepository
import io.clroot.ball.domain.model.core.BinaryId
import io.clroot.ball.shared.attribute.Attributable
import io.clroot.ball.shared.attribute.AttributeKey
import io.clroot.ball.shared.attribute.AttributeSerializer
import io.clroot.ball.shared.attribute.AttributeStore
import io.clroot.ball.shared.core.model.Entity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class JpaAttributePersistenceProviderTest : StringSpec({
    // Setup mocks
    val attributeRepository = mockk<AttributeJpaRepository>()
    val attributeSerializer = mockk<AttributeSerializer>()
    val provider = JpaAttributePersistenceProvider(attributeRepository, attributeSerializer)

    beforeTest {
        clearAllMocks()
    }

    "loadAttributes should load attributes from repository to entity" {
        // Given
        val entity = TestEntity()
        val entityId = entity.id.toString()
        val entityType = entity.javaClass.simpleName
        val attributeEntities = listOf(
            AttributeEntity(
                id = BinaryId.new(),
                entityId = entityId,
                entityType = entityType,
                attrKey = "stringKey",
                attrValue = "\"stringValue\"",
                valueType = "java.lang.String"
            ),
            AttributeEntity(
                id = BinaryId.new(),
                entityId = entityId,
                entityType = entityType,
                attrKey = "intKey",
                attrValue = "42",
                valueType = "java.lang.Integer"
            )
        )

        // Mock repository and serializer behavior
        every { attributeRepository.findAllByEntityIdAndEntityType(entityId, entityType) } returns attributeEntities
        every { attributeSerializer.deserialize<Any>("\"stringValue\"", "java.lang.String") } returns "stringValue"
        every { attributeSerializer.deserialize<Any>("42", "java.lang.Integer") } returns 42

        // When
        val loadedEntity = provider.loadAttributes(entity, Any())

        // Then
        loadedEntity.getAttribute(AttributeKey("stringKey", String::class)) shouldBe "stringValue".some()
        loadedEntity.getAttribute(AttributeKey("intKey", Int::class)) shouldBe 42.some()
        verify { attributeRepository.findAllByEntityIdAndEntityType(entityId, entityType) }
        verify { attributeSerializer.deserialize<Any>("\"stringValue\"", "java.lang.String") }
        verify { attributeSerializer.deserialize<Any>("42", "java.lang.Integer") }
    }

    "loadAttributes should return entity unchanged if it's not Attributable" {
        // Given
        val entity = NonAttributableEntity()

        // When
        val loadedEntity = provider.loadAttributes(entity, Any())

        // Then
        loadedEntity shouldBe entity
    }

    "saveAttributes should save attributes from entity to repository" {
        // Given
        val stringKey = AttributeKey("stringKey", String::class)
        val intKey = AttributeKey("intKey", Int::class)
        val entity = TestEntity()
            .setAttribute(stringKey, "stringValue")
            .setAttribute(intKey, 42)
        val entityId = entity.id.toString()
        val entityType = entity.javaClass.simpleName

        // Mock repository and serializer behavior
        every { attributeRepository.deleteAllByEntityIdAndEntityType(entityId, entityType) } returns Unit
        every { attributeSerializer.serialize("stringValue") } returns "\"stringValue\""
        every { attributeSerializer.serialize(42) } returns "42"
        every { attributeRepository.saveAll(any<List<AttributeEntity>>()) } returns listOf(
            AttributeEntity(
                id = BinaryId.new(),
                entityId = entityId,
                entityType = entityType,
                attrKey = "stringKey",
                attrValue = "\"stringValue\"",
                valueType = "java.lang.String"
            ),
            AttributeEntity(
                id = BinaryId.new(),
                entityId = entityId,
                entityType = entityType,
                attrKey = "intKey",
                attrValue = "42",
                valueType = "java.lang.Integer"
            )
        )

        // When
        provider.saveAttributes(entity, Any())

        // Then
        verify { attributeRepository.deleteAllByEntityIdAndEntityType(entityId, entityType) }
        verify { attributeSerializer.serialize("stringValue") }
        verify { attributeSerializer.serialize(42) }
        verify { attributeRepository.saveAll(any<List<AttributeEntity>>()) }
    }

    "saveAttributes should do nothing if entity is not Attributable" {
        // Given
        val entity = NonAttributableEntity()

        // When
        provider.saveAttributes(entity, Any())

        // Then
        verify(exactly = 0) { attributeRepository.deleteAllByEntityIdAndEntityType(any(), any()) }
        verify(exactly = 0) { attributeRepository.saveAll(any<List<AttributeEntity>>()) }
    }
})

// Test entity for persistence tests
private class TestEntity(
    override val id: UUID = UUID.randomUUID(),
    override val attributes: AttributeStore = AttributeStore.empty()
) : Entity<UUID>, Attributable<TestEntity> {
    override fun <V : Any> setAttribute(key: AttributeKey<V>, value: V): TestEntity {
        return TestEntity(id, attributes.setAttribute(key, value))
    }

    override fun unsafeSetAttributes(attributes: AttributeStore): TestEntity {
        return TestEntity(id, attributes)
    }
}

// Non-attributable entity for testing
private class NonAttributableEntity(
    override val id: UUID = UUID.randomUUID()
) : Entity<UUID>
