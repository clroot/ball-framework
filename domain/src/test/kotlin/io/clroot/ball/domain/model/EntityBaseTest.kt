package io.clroot.ball.domain.model

import io.clroot.ball.domain.model.core.EntityBase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class EntityBaseTest : FunSpec({
    // Create a concrete implementation of EntityBase for testing
    class TestEntity(id: String) : EntityBase<String>(id)

    test("equals() should return true for entities with the same ID") {
        val entity1 = TestEntity("1")
        val entity2 = TestEntity("1")

        entity1 shouldBe entity2
    }

    test("equals() should return false for entities with different IDs") {
        val entity1 = TestEntity("1")
        val entity2 = TestEntity("2")

        entity1 shouldNotBe entity2
    }

    test("equals() should return false for different entity types") {
        val entity1 = TestEntity("1")
        val entity2 = object : EntityBase<String>("1") {}

        entity1 shouldNotBe entity2
    }

    test("hashCode() should return the same value for entities with the same ID") {
        val entity1 = TestEntity("1")
        val entity2 = TestEntity("1")

        entity1.hashCode() shouldBe entity2.hashCode()
    }

    test("hashCode() should return different values for entities with different IDs") {
        val entity1 = TestEntity("1")
        val entity2 = TestEntity("2")

        entity1.hashCode() shouldNotBe entity2.hashCode()
    }

    test("toString() should include the class name and ID") {
        val entity = TestEntity("1")

        entity.toString() shouldBe "TestEntity(id=1)"
    }
})
