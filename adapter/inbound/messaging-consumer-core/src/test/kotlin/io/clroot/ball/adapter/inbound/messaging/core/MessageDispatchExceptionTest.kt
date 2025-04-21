package io.clroot.ball.adapter.inbound.messaging.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageDispatchExceptionTest {

    @Test
    fun `constructor should initialize properties correctly`() {
        // Given
        val message = "Test error message"
        val cause = RuntimeException("Test cause")
        val messageId = "test-message-id"
        val topic = "test-topic"
        val retryable = true

        // When
        val exception = MessageDispatchException(
            message = message,
            cause = cause,
            messageId = messageId,
            topic = topic,
            retryable = retryable
        )

        // Then
        assertEquals(messageId, exception.messageId)
        assertEquals(topic, exception.topic)
        assertEquals(retryable, exception.retryable)
        assertEquals(cause, exception.cause)
        assertTrue(exception.message!!.contains(message))
        assertTrue(exception.message!!.contains(messageId))
        assertTrue(exception.message!!.contains(topic))
        assertTrue(exception.message!!.contains("retryable=true"))
    }

    @Test
    fun `constructor should use default values for optional parameters`() {
        // Given
        val message = "Test error message"

        // When
        val exception = MessageDispatchException(message)

        // Then
        assertNull(exception.messageId)
        assertNull(exception.topic)
        assertTrue(exception.retryable) // Default is true
        assertNull(exception.cause)
        assertEquals(message + " [retryable=true]", exception.message)
    }

    @Test
    fun `nonRetryable should create exception with retryable set to false`() {
        // Given
        val message = "Test error message"
        val cause = RuntimeException("Test cause")
        val messageId = "test-message-id"
        val topic = "test-topic"

        // When
        val exception = MessageDispatchException.nonRetryable(
            message = message,
            cause = cause,
            messageId = messageId,
            topic = topic
        )

        // Then
        assertEquals(messageId, exception.messageId)
        assertEquals(topic, exception.topic)
        assertFalse(exception.retryable)
        assertEquals(cause, exception.cause)
        assertTrue(exception.message!!.contains(message))
        assertTrue(exception.message!!.contains(messageId))
        assertTrue(exception.message!!.contains(topic))
        assertTrue(exception.message!!.contains("retryable=false"))
    }

    @Test
    fun `message should include all provided details`() {
        // Given
        val message = "Test error message"
        val messageId = "test-message-id"
        val topic = "test-topic"
        val retryable = true

        // When
        val exception = MessageDispatchException(
            message = message,
            messageId = messageId,
            topic = topic,
            retryable = retryable
        )

        // Then
        val expectedMessage = "$message [messageId=$messageId, topic=$topic, retryable=$retryable]"
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `message should handle null details`() {
        // Given
        val message = "Test error message"

        // When
        val exception = MessageDispatchException(
            message = message,
            messageId = null,
            topic = null
        )

        // Then
        val expectedMessage = "$message [retryable=true]"
        assertEquals(expectedMessage, exception.message)
    }
}