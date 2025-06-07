package io.clroot.ball.adapter.inbound.rest.exception

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DebugInfo(
    val path: String? = null,

    val method: String? = null,

    val exceptionType: String? = null,

    val stackTrace: String? = null,

    val location: String? = null
)