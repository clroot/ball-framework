package io.clroot.ball.adapter.inbound.messaging.core

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MessageMetadataTest {

    @Test
    fun `constructor should initialize properties correctly`() {
        // Given
        val messageId = "test-message-id"
        val timestamp = Instant.now()
        val headers = mapOf("key1" to "value1", "key2" to "value2")
        val source = "test-source"
        val eventType = "test-event"

        // When
        val metadata = MessageMetadata(
            messageId = messageId,
            timestamp = timestamp,
            headers = headers,
            source = source,
            eventType = eventType
        )

        // Then
        assertEquals(messageId, metadata.messageId)
        assertEquals(timestamp, metadata.timestamp)
        assertEquals(headers, metadata.headers)
        assertEquals(source, metadata.source)
        assertEquals(eventType, metadata.eventType)
    }

    @Test
    fun `constructor should use default values for optional parameters`() {
        // Given
        val messageId = "test-message-id"
        val timestamp = Instant.now()

        // When
        val metadata = MessageMetadata(
            messageId = messageId,
            timestamp = timestamp
        )

        // Then
        assertEquals(messageId, metadata.messageId)
        assertEquals(timestamp, metadata.timestamp)
        assertEquals(emptyMap(), metadata.headers)
        assertNull(metadata.source)
        assertNull(metadata.eventType)
    }

    @Test
    fun `createDefault should create metadata with generated messageId and current timestamp`() {
        // When
        val metadata = MessageMetadata.createDefault()

        // Then
        assertNotNull(metadata.messageId)
        assertNotNull(metadata.timestamp)
        assertEquals(emptyMap(), metadata.headers)
        assertNull(metadata.source)
        assertNull(metadata.eventType)
    }

    @Test
    fun `createDefault should use provided messageId and timestamp`() {
        // Given
        val messageId = "custom-message-id"
        val timestamp = Instant.now().minusSeconds(60) // 1 minute ago

        // When
        val metadata = MessageMetadata.createDefault(
            messageId = messageId,
            timestamp = timestamp
        )

        // Then
        assertEquals(messageId, metadata.messageId)
        assertEquals(timestamp, metadata.timestamp)
        assertEquals(emptyMap(), metadata.headers)
        assertNull(metadata.source)
        assertNull(metadata.eventType)
    }
}