package io.clroot.ball.shared.attribute

import arrow.core.none
import arrow.core.some
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

class AttributeStoreTest : StringSpec({
    "empty() should create an empty AttributeStore" {
        val store = AttributeStore.empty()
        store.getAttributes().shouldBeEmpty()
    }

    "of() should create an AttributeStore with the given attributes" {
        val key1 = AttributeKey("key1", String::class)
        val key2 = AttributeKey("key2", Int::class)
        val store = AttributeStore.of(key1 to "value1", key2 to 42)

        store.getAttributes() shouldContainExactly mapOf(key1 to "value1", key2 to 42)
    }

    "getAttribute() should return Some when attribute exists" {
        val key = AttributeKey("key", String::class)
        val store = AttributeStore.of(key to "value")

        val result = store.getAttribute(key)
        result shouldBe "value".some()
    }

    "getAttribute() should return None when attribute does not exist" {
        val key = AttributeKey("key", String::class)
        val store = AttributeStore.empty()

        val result = store.getAttribute(key)
        result shouldBe none()
    }

    "getAttribute() should return None when attribute exists with wrong type" {
        // Create a key with the same name but different type
        val stringKey = AttributeKey("key", String::class)
        val intKey = AttributeKey("key", Int::class)

        // Store an Int value with the intKey
        val store = AttributeStore.of(intKey to 42)

        // Try to get a String value with the stringKey
        val result = store.getAttribute(stringKey)

        // Since the keys have the same name, the value is found in the map
        // But since the types don't match, the cast fails and None is returned
        result shouldBe none()
    }

    "setAttribute() should add a new attribute" {
        val key = AttributeKey("key", String::class)
        val store = AttributeStore.empty()

        val newStore = store.setAttribute(key, "value")
        newStore.getAttribute(key) shouldBe "value".some()
    }

    "setAttribute() should update an existing attribute" {
        val key = AttributeKey("key", String::class)
        val store = AttributeStore.of(key to "oldValue")

        val newStore = store.setAttribute(key, "newValue")
        newStore.getAttribute(key) shouldBe "newValue".some()
    }

    "setAttribute() should not modify the original store" {
        val key = AttributeKey("key", String::class)
        val store = AttributeStore.empty()

        store.setAttribute(key, "value")
        store.getAttribute(key) shouldBe none()
    }

    "getAttributes() should return all attributes" {
        val key1 = AttributeKey("key1", String::class)
        val key2 = AttributeKey("key2", Int::class)
        val store = AttributeStore.of(key1 to "value1", key2 to 42)

        store.getAttributes() shouldContainExactly mapOf(key1 to "value1", key2 to 42)
    }
})
