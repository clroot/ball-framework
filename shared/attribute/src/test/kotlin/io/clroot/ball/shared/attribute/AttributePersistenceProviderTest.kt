package io.clroot.ball.shared.attribute

import arrow.core.some
import io.clroot.ball.shared.core.model.Entity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.*

class AttributePersistenceProviderTest : StringSpec({
    "loadAttributes should load attributes from data model to entity" {
        val provider = TestAttributePersistenceProvider()
        val entity = PersistenceTestEntity()
        val dataModel = TestDataModel()
        dataModel.attributes["key1"] = "value1"
        dataModel.attributes["key2"] = "42"

        val loadedEntity = provider.loadAttributes(entity, dataModel)

        loadedEntity.getAttribute(AttributeKey("key1", String::class)) shouldBe "value1".some()
        loadedEntity.getAttribute(AttributeKey("key2", Int::class)) shouldBe 42.some()
    }

    "saveAttributes should save attributes from entity to data model" {
        val provider = TestAttributePersistenceProvider()
        val key1 = AttributeKey("key1", String::class)
        val key2 = AttributeKey("key2", Int::class)
        val entity = PersistenceTestEntity()
            .setAttribute(key1, "value1")
            .setAttribute(key2, 42)
        val dataModel = TestDataModel()

        provider.saveAttributes(entity, dataModel)

        dataModel.attributes["key1"] shouldBe "value1"
        dataModel.attributes["key2"] shouldBe "42" // Stored as string
    }
})

// Test implementation of AttributePersistenceProvider
private class TestAttributePersistenceProvider : AttributePersistenceProvider {
    override fun <E : Entity<*>> loadAttributes(entity: E, dataModel: Any): E {
        if (entity !is Attributable<*> || dataModel !is TestDataModel) {
            return entity
        }

        var result = entity
        dataModel.attributes.forEach { (key, value) ->
            when (key) {
                "key1" -> {
                    @Suppress("UNCHECKED_CAST")
                    result = (result as Attributable<E>).setAttribute(
                        AttributeKey(key, String::class),
                        value
                    )
                }

                "key2" -> {
                    @Suppress("UNCHECKED_CAST")
                    result = (result as Attributable<E>).setAttribute(
                        AttributeKey(key, Int::class),
                        value.toInt()
                    )
                }
            }
        }

        return result
    }

    override fun <E : Entity<*>> saveAttributes(entity: E, dataModel: Any) {
        if (entity !is Attributable<*> || dataModel !is TestDataModel) {
            return
        }

        entity.attributes.getAttributes().forEach { (key, value) ->
            dataModel.attributes[key.name] = value.toString()
        }
    }
}

// Test data model
private class TestDataModel {
    val attributes: MutableMap<String, String> = mutableMapOf()
}

// Test entity for persistence tests
private class PersistenceTestEntity(
    override val id: UUID = UUID.randomUUID(),
    override val attributes: AttributeStore = AttributeStore.empty()
) : Entity<UUID>, Attributable<PersistenceTestEntity> {

    override fun <V : Any> setAttribute(key: AttributeKey<V>, value: V): PersistenceTestEntity {
        return PersistenceTestEntity(id, attributes.setAttribute(key, value))
    }
}
