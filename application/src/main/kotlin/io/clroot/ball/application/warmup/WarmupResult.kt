package io.clroot.ball.application.warmup

/**
 * Warm-up 결과
 */
data class WarmupResult(
    val taskName: String,
    val success: Boolean,
    val durationMs: Long,
    val error: String?,
)
