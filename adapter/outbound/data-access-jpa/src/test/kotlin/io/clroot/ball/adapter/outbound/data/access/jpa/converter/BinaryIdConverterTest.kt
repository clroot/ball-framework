package io.clroot.ball.adapter.outbound.data.access.jpa.converter

import io.clroot.ball.domain.exception.DomainValidationException
import io.clroot.ball.domain.model.vo.BinaryId
import io.kotest.assertions.throwables.shouldThrow
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
    
    "should throw InvalidIdException for invalid byte arrays" {
        // Given
        val invalidBytes = ByteArray(8) // Wrong size (expected: 16 bytes)
        
        // When & Then
        shouldThrow<DomainValidationException> {
            converter.convertToEntityAttribute(invalidBytes)
        }
    }
})
