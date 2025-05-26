package io.clroot.ball.adapter.outbound.persistence.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.sql.Timestamp
import java.time.Instant

/**
 * JPA converter for Instant to store it as TIMESTAMP in the database
 */
@Converter(autoApply = true)
class InstantConverter : AttributeConverter<Instant, Timestamp> {
    /**
     * Converts Instant to Timestamp for database storage
     */
    override fun convertToDatabaseColumn(attribute: Instant?): Timestamp? {
        return attribute?.let { Timestamp.from(it) }
    }

    /**
     * Converts Timestamp from database to Instant
     */
    override fun convertToEntityAttribute(dbData: Timestamp?): Instant? {
        return dbData?.toInstant()
    }
}