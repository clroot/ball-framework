package io.clroot.ball.shared.attribute

interface AttributeSerializer {
    fun serialize(value: Any): String

    fun <T : Any> deserialize(value: String, type: String): T?
}
