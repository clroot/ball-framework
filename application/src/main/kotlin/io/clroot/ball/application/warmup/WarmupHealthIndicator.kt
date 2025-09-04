package io.clroot.ball.application.warmup

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component

/**
 * Warm-up 상태를 Spring Actuator Health Endpoint를 통해 노출하는 인디케이터
 *
 * /actuator/health/warmup 엔드포인트로 warm-up 상태를 확인할 수 있다.
 */
@Component("warmup")
@ConditionalOnClass(HealthIndicator::class)
class WarmupHealthIndicator(
    private val warmupService: WarmupService,
) : HealthIndicator {
    override fun health(): Health {
        val warmupStatus = warmupService.getAllWarmupStatus()

        if (warmupStatus.isEmpty()) {
            return Health
                .unknown()
                .withDetail("status", "No warm-up tasks registered")
                .build()
        }

        val successCount = warmupStatus.values.count { it.success }
        val totalCount = warmupStatus.size
        val allSuccess = successCount == totalCount

        val builder =
            if (allSuccess) {
                Health.up()
            } else {
                Health.down()
            }

        return builder
            .withDetail("successCount", successCount)
            .withDetail("totalCount", totalCount)
            .withDetail("successRate", String.format("%.1f%%", (successCount.toDouble() / totalCount) * 100))
            .withDetails(
                warmupStatus.mapValues { (_, result) ->
                    mapOf(
                        "success" to result.success,
                        "durationMs" to result.durationMs,
                        "error" to result.error,
                    )
                },
            ).build()
    }
}
