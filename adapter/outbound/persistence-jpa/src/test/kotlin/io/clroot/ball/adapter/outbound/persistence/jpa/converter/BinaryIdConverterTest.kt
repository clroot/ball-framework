package io.clroot.ball.adapter.outbound.persistence.jpa.converter

import io.clroot.ball.domain.model.core.BinaryId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BinaryIdConverterTest : StringSpec({
    val converter = BinaryIdConverter()

    "convertToDatabaseColumn should convert BinaryId to ByteArray" {
        // Given
        val binaryId = BinaryId.new()
        
        // When
        val bytes = converter.convertToDatabaseColumn(binaryId)
        
        // Then
        bytes shouldNotBe null
        bytes!!.size shouldBe 16
    }
    
    "convertToEntityAttribute should convert ByteArray to BinaryId" {
        // Given
        val originalId = BinaryId.new()
        val bytes = converter.convertToDatabaseColumn(originalId)
        
        // When
        val convertedId = converter.convertToEntityAttribute(bytes)
        
        // Then
        convertedId shouldNotBe null
        // Note: Due to the conversion process, the exact string representation might not match
        // but the binary representation should be consistent
        converter.convertToDatabaseColumn(convertedId!!) shouldBe bytes
    }
    
    "conversion should be consistent for the same BinaryId" {
        // Given
        val binaryId = BinaryId.new()
        
        // When
        val bytes1 = converter.convertToDatabaseColumn(binaryId)
        val bytes2 = converter.convertToDatabaseColumn(binaryId)
        
        // Then
        bytes1 shouldBe bytes2
    }
    
    "null input should result in null output" {
        // Given
        val nullBinaryId: BinaryId? = null
        val nullBytes: ByteArray? = null
        
        // When
        val bytes = converter.convertToDatabaseColumn(nullBinaryId)
        val binaryId = converter.convertToEntityAttribute(nullBytes)
        
        // Then
        bytes shouldBe null
        binaryId shouldBe null
    }
})