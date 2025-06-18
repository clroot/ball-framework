package io.clroot.ball.adapter.outbound.data.access.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * JPA converter for LocalDateTime to store it as TIMESTAMP in the database
 */
@Converter(autoApply = true)
class LocalDateTimeConverter : AttributeConverter<LocalDateTime, Timestamp> {
    /**
     * Converts LocalDateTime to Timestamp for database storage
     */
    override fun convertToDatabaseColumn(attribute: LocalDateTime?): Timestamp? = attribute?.let { Timestamp.valueOf(it) }

    /**
     * Converts Timestamp from database to LocalDateTime
     */
    override fun convertToEntityAttribute(dbData: Timestamp?): LocalDateTime? = dbData?.toLocalDateTime()
}
