package io.clroot.ball.adapter.inbound.rest.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,

    val message: String,

    val timestamp: Instant = Instant.now(),

    val traceId: String? = null,

    val details: Map<String, Any>? = null,

    val debug: DebugInfo? = null
)