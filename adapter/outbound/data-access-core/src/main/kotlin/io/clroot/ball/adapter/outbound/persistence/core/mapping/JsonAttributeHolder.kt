package io.clroot.ball.adapter.outbound.persistence.core.mapping

/**
 * Marker interface for record classes that can store attributes as JSON
 *
 * This interface is used to identify record classes that can store attributes as JSON.
 * It is used by the JsonAttributePersistenceProvider to determine if an record can store attributes.
 */
interface JsonAttributeHolder {
    /**
     * The JSON string representation of the attributes
     */
    var attributes: String?
}