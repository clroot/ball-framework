package io.clroot.ball.adapter.inbound.messaging.core

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageConsumerRegistryTest {

    private lateinit var registry: MessageConsumerRegistry

    @BeforeEach
    fun setUp() {
        registry = MessageConsumerRegistry()
    }

    @Test
    fun `registerConsumer should add consumer to registry`() {
        // Given
        val consumer = mock(MessageConsumer::class.java)
        `when`(consumer.getTopicName()).thenReturn("test-topic")

        // When
        registry.registerConsumer(consumer)

        // Then
        assertEquals(1, registry.size())
        assertEquals(setOf("test-topic"), registry.getRegisteredTopics())
    }

    @Test
    fun `registerConsumer should throw exception when topic is blank`() {
        // Given
        val consumer = mock(MessageConsumer::class.java)
        `when`(consumer.getTopicName()).thenReturn("")

        // When/Then
        assertThrows<IllegalArgumentException> {
            registry.registerConsumer(consumer)
        }
    }

    @Test
    fun `registerConsumer should throw exception when topic is already registered`() {
        // Given
        val consumer1 = mock(MessageConsumer::class.java)
        val consumer2 = mock(MessageConsumer::class.java)
        `when`(consumer1.getTopicName()).thenReturn("test-topic")
        `when`(consumer2.getTopicName()).thenReturn("test-topic")

        // When
        registry.registerConsumer(consumer1)

        // Then
        assertThrows<IllegalArgumentException> {
            registry.registerConsumer(consumer2)
        }
    }

    @Test
    fun `getConsumer should return registered consumer`() {
        // Given
        val consumer = mock(MessageConsumer::class.java)
        `when`(consumer.getTopicName()).thenReturn("test-topic")
        registry.registerConsumer(consumer)

        // When
        val result = registry.getConsumer<Any>("test-topic")

        // Then
        assertEquals(consumer, result)
    }

    @Test
    fun `getConsumer should return null for unregistered topic`() {
        // Given
        val consumer = mock(MessageConsumer::class.java)
        `when`(consumer.getTopicName()).thenReturn("test-topic")
        registry.registerConsumer(consumer)

        // When
        val result = registry.getConsumer<Any>("unknown-topic")

        // Then
        assertNull(result)
    }

    @Test
    fun `unregisterConsumer should remove consumer from registry`() {
        // Given
        val consumer = mock(MessageConsumer::class.java)
        `when`(consumer.getTopicName()).thenReturn("test-topic")
        registry.registerConsumer(consumer)

        // When
        val removed = registry.unregisterConsumer<Any>("test-topic")

        // Then
        assertEquals(consumer, removed)
        assertEquals(0, registry.size())
        assertTrue(registry.getRegisteredTopics().isEmpty())
    }

    @Test
    fun `unregisterConsumer should return null for unregistered topic`() {
        // Given
        val consumer = mock(MessageConsumer::class.java)
        `when`(consumer.getTopicName()).thenReturn("test-topic")
        registry.registerConsumer(consumer)

        // When
        val removed = registry.unregisterConsumer<Any>("unknown-topic")

        // Then
        assertNull(removed)
        assertEquals(1, registry.size())
    }

    @Test
    fun `clear should remove all consumers`() {
        // Given
        val consumer1 = mock(MessageConsumer::class.java)
        val consumer2 = mock(MessageConsumer::class.java)
        `when`(consumer1.getTopicName()).thenReturn("topic1")
        `when`(consumer2.getTopicName()).thenReturn("topic2")
        registry.registerConsumer(consumer1)
        registry.registerConsumer(consumer2)

        // When
        registry.clear()

        // Then
        assertEquals(0, registry.size())
        assertTrue(registry.getRegisteredTopics().isEmpty())
    }
}