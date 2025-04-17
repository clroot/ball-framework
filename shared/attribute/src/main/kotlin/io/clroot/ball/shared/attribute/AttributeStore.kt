package io.clroot.ball.shared.attribute

import arrow.core.Option

class AttributeStore private constructor(
    private val attributes: Map<AttributeKey<*>, Any> = mapOf()
) : AttributeHolder {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAttribute(key: AttributeKey<T>): Option<T> {
        val value = attributes[key] ?: return Option.fromNullable(null)

        return if (key.type.isInstance(value)) {
            Option.fromNullable(value as? T) // 안전한 캐스트 시도
        } else {
            Option.fromNullable(null)
        }
    }

    override fun <T : Any> setAttribute(key: AttributeKey<T>, value: T): AttributeStore {
        return AttributeStore(attributes + (key to value))
    }


    override fun getAttributes(): Map<AttributeKey<*>, Any> = attributes

    companion object {
        fun empty(): AttributeStore = AttributeStore()

        fun of(vararg pairs: Pair<AttributeKey<*>, Any>): AttributeStore {
            val attributes = mutableMapOf<AttributeKey<*>, Any>()
            for ((key, value) in pairs) {
                val keyType = key.type // Unchecked cast

                if (!keyType.isInstance(value)) {
                    throw IllegalArgumentException(
                        "Type mismatch for key '${key.name}'. " +
                                "Expected type ${keyType.simpleName} but got value of type ${value::class.simpleName}."
                    )
                }
                attributes[key] = value
            }
            return AttributeStore(attributes.toMap())
        }
    }
}