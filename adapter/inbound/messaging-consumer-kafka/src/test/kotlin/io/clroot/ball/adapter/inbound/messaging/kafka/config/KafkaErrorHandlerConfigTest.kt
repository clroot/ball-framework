package io.clroot.ball.adapter.inbound.messaging.kafka.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.util.backoff.ExponentialBackOff

class KafkaErrorHandlerConfigTest : FunSpec({

    test("backOff should create an ExponentialBackOff with correct properties") {
        // Given
        val properties = KafkaConsumerProperties(defaultRetryCount = 5)
        val config = KafkaErrorHandlerConfig(properties)

        // When
        val backOff = config.backOff()

        // Then
        backOff.shouldBeInstanceOf<ExponentialBackOff>()

        // Access the private fields via reflection
        val initialIntervalField = ExponentialBackOff::class.java.getDeclaredField("initialInterval")
        initialIntervalField.isAccessible = true
        val initialInterval = initialIntervalField.get(backOff) as Long

        val multiplierField = ExponentialBackOff::class.java.getDeclaredField("multiplier")
        multiplierField.isAccessible = true
        val multiplier = multiplierField.get(backOff) as Double

        val maxAttemptsField = ExponentialBackOff::class.java.getDeclaredField("maxAttempts")
        maxAttemptsField.isAccessible = true
        val maxAttempts = maxAttemptsField.get(backOff) as Int

        initialInterval shouldBe 1000L
        multiplier shouldBe 2.0
        maxAttempts shouldBe 5L
    }
})