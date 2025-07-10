package io.clroot.ball.shared.jackson

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun <T : Any> T.toJsonString(): String {
    val mapper = jacksonObjectMapper()
    return mapper.writeValueAsString(this)
}
