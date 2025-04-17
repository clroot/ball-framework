package io.clroot.ball.shared.attribute

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AttributeSerializerTest : StringSpec({
    "serialize should convert value to string" {
        val serializer = TestAttributeSerializer()
        val value = "test"

        val result = serializer.serialize(value)

        result shouldBe "test"
    }

    "deserialize should convert string to value of correct type" {
        val serializer = TestAttributeSerializer()
        val value = "test"

        val result = serializer.deserialize<String>(value, "java.lang.String")

        result shouldBe "test"
    }

    "deserialize should return null for unknown type" {
        val serializer = TestAttributeSerializer()
        val value = "test"

        val result = serializer.deserialize<Any>(value, "unknown.type")

        result shouldBe null
    }
})

// Simple test implementation of AttributeSerializer
private class TestAttributeSerializer : AttributeSerializer {
    override fun serialize(value: Any): String = value.toString()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserialize(value: String, type: String): T? {
        return when (type) {
            "java.lang.String" -> value as T
            "java.lang.Integer", "int" -> value.toIntOrNull() as T?
            "java.lang.Long", "long" -> value.toLongOrNull() as T?
            "java.lang.Boolean", "boolean" -> value.toBoolean() as T?
            else -> null
        }
    }
}
