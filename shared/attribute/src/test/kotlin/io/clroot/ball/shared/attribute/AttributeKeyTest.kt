package io.clroot.ball.shared.attribute

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AttributeKeyTest : StringSpec({
    "AttributeKey should have correct name" {
        val key = AttributeKey("test", String::class)
        key.name shouldBe "test"
    }

    "AttributeKey with same name should be equal" {
        val key1 = AttributeKey("test", String::class)
        val key2 = AttributeKey("test", String::class)
        key1 shouldBe key2
        key1.hashCode() shouldBe key2.hashCode()
    }

    "AttributeKey with different name should not be equal" {
        val key1 = AttributeKey("test1", String::class)
        val key2 = AttributeKey("test2", String::class)
        key1 shouldNotBe key2
        key1.hashCode() shouldNotBe key2.hashCode()
    }

    "AttributeKey with same name but different type should be equal" {
        val key1 = AttributeKey("test", String::class)
        val key2 = AttributeKey("test", Int::class)
        key1 shouldBe key2
        key1.hashCode() shouldBe key2.hashCode()
    }

    "toString should return correct representation" {
        val key = AttributeKey("test", String::class)
        key.toString() shouldBe "AttributeKey(test, type=String)"
    }
})