package io.clroot.ball.adapter.outbound.data.access.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class ClassTypeConverter : AttributeConverter<Class<*>?, String?> {
    override fun convertToDatabaseColumn(attribute: Class<*>?): String? = attribute?.name

    override fun convertToEntityAttribute(dbData: String?): Class<*> =
        runCatching {
            dbData?.let {
                Class.forName(it)
            }
        }.getOrNull() ?: throw IllegalStateException("Cannot convert $dbData to Class type")
}
