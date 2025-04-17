package io.clroot.ball.shared.attribute

import arrow.core.Option

interface AttributeHolder {
    fun <T : Any> getAttribute(key: AttributeKey<T>): Option<T>

    fun <T : Any> setAttribute(key: AttributeKey<T>, value: T): AttributeHolder

    fun getAttributes(): Map<AttributeKey<*>, Any>
}