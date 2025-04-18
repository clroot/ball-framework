package io.clroot.ball.adapter.inbound.messaging.core.retry

import io.clroot.ball.adapter.inbound.messaging.core.MessageDispatchException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class RetryPolicyTest {

    @Test
    fun `constructor should initialize properties correctly`() {
        // Given
        val maxRetries = 5
        val initialDelayMs = 2000L
        val maxDelayMs = 20000L
        val backoffMultiplier = 3.0

        // When
        val policy = RetryPolicy(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            backoffMultiplier = backoffMultiplier
        )

        // Then
        assertEquals(maxRetries, policy.maxRetries)
        assertEquals(initialDelayMs, policy.initialDelayMs)
        assertEquals(maxDelayMs, policy.maxDelayMs)
        assertEquals(backoffMultiplier, policy.backoffMultiplier)
    }

    @Test
    fun `constructor should use default values`() {
        // When
        val policy = RetryPolicy()

        // Then
        assertEquals(3, policy.maxRetries)
        assertEquals(1000L, policy.initialDelayMs)
        assertEquals(10000L, policy.maxDelayMs)
        assertEquals(2.0, policy.backoffMultiplier)
    }

    @Test
    fun `constructor should validate maxRetries`() {
        // Then
        assertThrows<IllegalArgumentException> {
            RetryPolicy(maxRetries = -1)
        }
    }

    @Test
    fun `constructor should validate initialDelayMs`() {
        // Then
        assertThrows<IllegalArgumentException> {
            RetryPolicy(initialDelayMs = 0)
        }
    }

    @Test
    fun `constructor should validate maxDelayMs`() {
        // Then
        assertThrows<IllegalArgumentException> {
            RetryPolicy(initialDelayMs = 2000, maxDelayMs = 1000)
        }
    }

    @Test
    fun `constructor should validate backoffMultiplier`() {
        // Then
        assertThrows<IllegalArgumentException> {
            RetryPolicy(backoffMultiplier = 0.5)
        }
    }

    @Test
    fun `calculateDelayDuration should return zero for retryCount less than or equal to 0`() {
        // Given
        val policy = RetryPolicy()

        // When
        val delay = policy.calculateDelayDuration(0)

        // Then
        assertEquals(0.milliseconds, delay)
    }

    @Test
    fun `calculateDelayDuration should calculate exponential backoff`() {
        // Given
        val policy = RetryPolicy(
            initialDelayMs = 1000,
            backoffMultiplier = 2.0
        )

        // When/Then
        assertEquals(1000.milliseconds, policy.calculateDelayDuration(1))
        assertEquals(2000.milliseconds, policy.calculateDelayDuration(2))
        assertEquals(4000.milliseconds, policy.calculateDelayDuration(3))
        assertEquals(8000.milliseconds, policy.calculateDelayDuration(4))
    }

    @Test
    fun `calculateDelayDuration should not exceed maxDelayMs`() {
        // Given
        val policy = RetryPolicy(
            initialDelayMs = 1000,
            maxDelayMs = 5000,
            backoffMultiplier = 3.0
        )

        // When/Then
        assertEquals(1000.milliseconds, policy.calculateDelayDuration(1))
        assertEquals(3000.milliseconds, policy.calculateDelayDuration(2))
        assertEquals(5000.milliseconds, policy.calculateDelayDuration(3)) // Would be 9000, but capped at 5000
        assertEquals(5000.milliseconds, policy.calculateDelayDuration(4)) // Would be 27000, but capped at 5000
    }

    @Test
    fun `isRetryable should return true for regular exceptions`() {
        // Given
        val policy = RetryPolicy()
        val exception = RuntimeException("Test exception")

        // When
        val result = policy.isRetryable(exception)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isRetryable should check retryable property for MessageDispatchException`() {
        // Given
        val policy = RetryPolicy()
        val retryableException = MessageDispatchException("Retryable", retryable = true)
        val nonRetryableException = MessageDispatchException("Non-retryable", retryable = false)

        // When/Then
        assertTrue(policy.isRetryable(retryableException))
        assertFalse(policy.isRetryable(nonRetryableException))
    }

    @Test
    fun `DEFAULT should have standard values`() {
        // When
        val policy = RetryPolicy.DEFAULT

        // Then
        assertEquals(3, policy.maxRetries)
        assertEquals(1000L, policy.initialDelayMs)
        assertEquals(10000L, policy.maxDelayMs)
        assertEquals(2.0, policy.backoffMultiplier)
    }

    @Test
    fun `NO_RETRY should have maxRetries set to 0`() {
        // When
        val policy = RetryPolicy.NO_RETRY

        // Then
        assertEquals(0, policy.maxRetries)
    }
}
