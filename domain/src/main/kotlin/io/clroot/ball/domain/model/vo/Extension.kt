package io.clroot.ball.domain.model.vo

import io.clroot.ball.shared.jackson.toJsonString

interface Extension {
    fun toJsonValue(): String = this.toJsonString()
}
