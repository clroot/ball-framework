package io.clroot.ball.domain.port

import io.clroot.ball.domain.model.vo.Extension

interface ExtensionParser<T : Extension> {
    fun parse(payload: String): T
}
