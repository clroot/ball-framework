package io.clroot.ball.adapter.outbound.persistence.jpa.converter

import io.clroot.ball.domain.model.vo.BinaryId
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.nio.ByteBuffer

/**
 * JPA converter for BinaryId to store it as BINARY(16) in the database
 */
@Converter(autoApply = true)
class BinaryIdConverter : AttributeConverter<BinaryId, ByteArray> {

    /**
     * Converts BinaryId to ByteArray for database storage
     * The ULID string is converted to a 16-byte binary representation
     */
    override fun convertToDatabaseColumn(attribute: BinaryId?): ByteArray? {
        if (attribute == null) return null

        // Convert ULID string to bytes using Base64 encoding
        // ULID is 26 characters in Crockford's base32, which is 128 bits (16 bytes)
        val ulidString = attribute.toString()

        // First decode from Crockford's base32 to binary
        // This is a simplified approach - in a real implementation, you'd use a proper base32 decoder
        val bytes = ByteArray(16)
        val buffer = ByteBuffer.wrap(bytes)

        // Convert first 10 characters (timestamp) - 48 bits
        val timestampPart = ulidString.substring(0, 10)
        val timestampValue = decodeBase32(timestampPart)
        buffer.putLong(0, timestampValue)

        // Convert remaining 16 characters (randomness) - 80 bits
        val randomPart = ulidString.substring(10)
        val randomValue1 = decodeBase32(randomPart.substring(0, 8))
        val randomValue2 = decodeBase32(randomPart.substring(8))

        // Write the remaining bits
        buffer.putInt(8, (randomValue1 shr 16).toInt())
        buffer.putShort(12, (randomValue1 and 0xFFFF).toShort())
        buffer.putShort(14, randomValue2.toShort())

        return bytes
    }

    /**
     * Converts ByteArray from database to BinaryId
     */
    override fun convertToEntityAttribute(dbData: ByteArray?): BinaryId? {
        if (dbData == null) return null

        // Ensure we have 16 bytes
        if (dbData.size != 16) {
            throw IllegalArgumentException("Binary data must be 16 bytes")
        }

        val buffer = ByteBuffer.wrap(dbData)

        // Extract timestamp (first 6 bytes / 48 bits)
        val timestamp = buffer.getLong(0) and 0xFFFFFFFFFFFF

        // Extract randomness (remaining 10 bytes / 80 bits)
        val random1 = (buffer.getInt(8).toLong() shl 16) or (buffer.getShort(12).toLong() and 0xFFFF)
        val random2 = buffer.getShort(14).toLong() and 0xFFFF

        // Convert to Crockford's base32 string
        val timestampStr = encodeBase32(timestamp, 10)
        val randomStr1 = encodeBase32(random1, 8)
        val randomStr2 = encodeBase32(random2, 8)

        val ulidString = timestampStr + randomStr1 + randomStr2

        // Create BinaryId from the ULID string
        return BinaryId.fromString(ulidString).getOrNull()
    }

    /**
     * Simplified Base32 (Crockford) decoder
     * In a real implementation, you would use a proper library
     */
    private fun decodeBase32(str: String): Long {
        val base32Chars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        var result = 0L

        for (c in str) {
            val value = base32Chars.indexOf(c)
            if (value == -1) throw IllegalArgumentException("Invalid base32 character: $c")
            result = (result shl 5) or value.toLong()
        }

        return result
    }

    /**
     * Simplified Base32 (Crockford) encoder
     * In a real implementation, you would use a proper library
     */
    private fun encodeBase32(value: Long, length: Int): String {
        val base32Chars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        val result = StringBuilder()

        var remaining = value
        for (i in 0 until length) {
            val index = (remaining and 0x1F).toInt()
            result.append(base32Chars[index])
            remaining = remaining shr 5
        }

        return result.reverse().toString()
    }
}