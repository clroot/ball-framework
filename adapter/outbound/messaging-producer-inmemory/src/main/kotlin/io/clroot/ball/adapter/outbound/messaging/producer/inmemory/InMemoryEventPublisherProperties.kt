package io.clroot.ball.adapter.outbound.messaging.producer.inmemory

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * InMemory Event Publisher configuration properties.
 * 
 * Configuration prefix: ball.event.publisher.inmemory
 * 
 * Example configuration:
 * ```yaml
 * ball:
 *   event:
 *     publisher:
 *       inmemory:
 *         async: true
 *         enable-retry: false
 *         max-retry-attempts: 3
 *         retry-delay-ms: 1000
 *         timeout-ms: 0
 *         enable-debug-logging: false
 * ```
 */
@ConfigurationProperties(prefix = "ball.event.publisher.inmemory")
data class InMemoryEventPublisherProperties(
    /**
     * Whether to publish events asynchronously.
     * 
     * When true, events are published asynchronously for better performance.
     * When false, events are published synchronously for easier debugging.
     * 
     * Default: true
     */
    val async: Boolean = true,

    /**
     * Whether to enable retry mechanism for failed event publishing.
     * 
     * When enabled, failed events will be retried according to the retry configuration.
     * 
     * Default: false
     */
    val enableRetry: Boolean = false,

    /**
     * Maximum number of retry attempts for failed events.
     * 
     * Only effective when enableRetry is true.
     * 
     * Default: 3
     */
    val maxRetryAttempts: Int = 3,

    /**
     * Delay between retry attempts in milliseconds.
     * 
     * The time to wait before retrying a failed event publication.
     * 
     * Default: 1000 (1 second)
     */
    val retryDelayMs: Long = 1000,

    /**
     * Event publishing timeout in milliseconds.
     * 
     * Maximum time to wait for event publishing to complete.
     * Set to 0 for no timeout.
     * 
     * Default: 0 (no timeout)
     */
    val timeoutMs: Long = 0,

    /**
     * Whether to enable debug logging for event publishing.
     * 
     * When enabled, detailed logs will be produced for event publishing operations.
     * Useful for debugging but may impact performance in production.
     * 
     * Default: false
     */
    val enableDebugLogging: Boolean = false
)
