package io.clroot.ball.adapter.outbound.data.access.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * JPA converter for Duration to store it as BIGINT in the database
 */
@Converter(autoApply = true)
class DurationConverter : AttributeConverter<Duration, Long> {
    override fun convertToDatabaseColumn(attribute: Duration?): Long = attribute?.toNanos() ?: 0L

    override fun convertToEntityAttribute(dbData: Long): Duration = Duration.of(dbData, ChronoUnit.NANOS)
}