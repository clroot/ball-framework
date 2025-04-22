package io.clroot.ball.shared.attribute

import arrow.core.none
import arrow.core.some
import io.clroot.ball.shared.core.model.Entity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.*

class AttributableTest : StringSpec({
    "getAttribute should return attribute value when it exists" {
        val entity = TestEntity()
        val key = AttributeKey("test", String::class)

        val updatedEntity = entity.setAttribute(key, "value")

        updatedEntity.getAttribute(key) shouldBe "value".some()
    }

    "getAttribute should return none when attribute does not exist" {
        val entity = TestEntity()
        val key = AttributeKey("test", String::class)

        entity.getAttribute(key) shouldBe none()
    }

    "setAttribute should return a new entity with the updated attribute" {
        val entity = TestEntity()
        val key = AttributeKey("test", String::class)

        val result = entity.setAttribute(key, "value")

        result.id shouldBe entity.id
        result.getAttribute(key) shouldBe "value".some()
    }
})

// Test implementation of Attributable
class TestEntity(
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
