package io.clroot.ball.domain.model.vo

import io.clroot.ball.shared.jackson.toJsonString

interface Extension {
    fun toJsonValue(): String = this.toJsonString()
}

inline fun <reified T : Extension> Extension.treat(): T? {
    if (this !is T) return null
    return this
}
