package io.clroot.ball.shared.attribute

data class AttributeKey<T : Any>(val name: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AttributeKey<*>
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
    
    override fun toString(): String = "AttributeKey($name)"
}
