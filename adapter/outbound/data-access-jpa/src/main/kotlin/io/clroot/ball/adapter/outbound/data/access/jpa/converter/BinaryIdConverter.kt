package io.clroot.ball.adapter.outbound.data.access.jpa.converter

import io.clroot.ball.domain.model.vo.BinaryId
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 이제 정말 간단한 JPA 컨버터!
 * 모든 로직은 BinaryId 내부에서 처리
 */
@Converter(autoApply = true)
class BinaryIdConverter : AttributeConverter<BinaryId, ByteArray> {

    override fun convertToDatabaseColumn(attribute: BinaryId?): ByteArray? {
        return attribute?.toBytes()
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): BinaryId? {
        if (dbData == null) return null
        return BinaryId.fromBytes(dbData).getOrNull()
    }
}
