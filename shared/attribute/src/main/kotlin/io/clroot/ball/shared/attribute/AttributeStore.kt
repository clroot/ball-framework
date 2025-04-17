package io.clroot.ball.shared.attribute

import arrow.core.Option

class AttributeStore private constructor(
    private val attributes: Map<AttributeKey<*>, Any> = mapOf()
) : AttributeHolder {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAttribute(key: AttributeKey<T>): Option<T> = Option.fromNullable(attributes[key] as? T)

    override fun <T : Any> setAttribute(key: AttributeKey<T>, value: T): AttributeStore =
        AttributeStore(attributes + (key to value))

    override fun getAttributes(): Map<AttributeKey<*>, Any> = attributes

    companion object {
        fun empty(): AttributeStore = AttributeStore()

        fun of(vararg pairs: Pair<AttributeKey<*>, Any>): AttributeStore = AttributeStore(pairs.toMap())
    }
}
