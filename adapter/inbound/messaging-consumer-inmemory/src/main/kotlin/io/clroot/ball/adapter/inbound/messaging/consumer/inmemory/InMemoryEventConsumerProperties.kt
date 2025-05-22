package io.clroot.ball.adapter.inbound.messaging.consumer.inmemory

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * InMemory Event Consumer configuration properties.
 * 
 * Configuration prefix: ball.event.consumer.inmemory
 * 
 * Example configuration:
 * ```yaml
 * ball:
 *   event:
 *     consumer:
 *       inmemory:
 *         enabled: true
 *         async: true
 *         parallel: true
 *         max-concurrency: 10
 *         timeout-ms: 5000
 *         enable-retry: false
 *         max-retry-attempts: 3
 *         retry-delay-ms: 1000
 *         enable-debug-logging: false
 * ```
 */
@ConfigurationProperties(prefix = "ball.event.consumer.inmemory")
data class InMemoryEventConsumerProperties(
    /**
     * Whether to enable the InMemory event consumer.
     * 
     * When false, the consumer will not be activated even if it's on the classpath.
     * 
     * Default: true
     */
    val enabled: Boolean = true,

    /**
     * Whether to process events asynchronously.
     * 
     * When true, events are processed asynchronously for better performance.
     * When false, events are processed synchronously for easier debugging.
     * 
     * Default: true
     */
    val async: Boolean = true,

    /**
     * Whether to process multiple handlers in parallel for the same event.
     * 
     * When true and multiple handlers exist for the same event type,
     * they will be executed in parallel for better performance.
     * 
     * Default: true
     */
    val parallel: Boolean = true,

    /**
     * Maximum number of concurrent handler executions.
     * 
     * Controls the maximum number of handlers that can be executed
     * simultaneously when parallel processing is enabled.
     * 
     * Default: 10
     */
    val maxConcurrency: Int = 10,

    /**
     * Event processing timeout in milliseconds.
     * 
     * Maximum time to wait for event processing to complete.
     * Set to 0 for no timeout.
     * 
     * Default: 5000 (5 seconds)
     */
    val timeoutMs: Long = 5000,

    /**
     * Whether to enable retry mechanism for failed event processing.
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
     * The time to wait before retrying a failed event processing.
     * 
     * Default: 1000 (1 second)
     */
    val retryDelayMs: Long = 1000,

    /**
     * Whether to enable debug logging for event processing.
     * 
     * When enabled, detailed logs will be produced for event processing operations.
     * Useful for debugging but may impact performance in production.
     * 
     * Default: false
     */
    val enableDebugLogging: Boolean = false
)
