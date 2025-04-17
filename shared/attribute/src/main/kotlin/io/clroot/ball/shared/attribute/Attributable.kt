package io.clroot.ball.shared.attribute

import arrow.core.Option
import io.clroot.ball.shared.core.model.Entity

interface Attributable<T : Entity<*>> {
    val attributes: AttributeStore

    fun <V : Any> getAttribute(key: AttributeKey<V>): Option<V> =
        attributes.getAttribute(key)

    fun <V : Any> setAttribute(key: AttributeKey<V>, value: V): T
}
