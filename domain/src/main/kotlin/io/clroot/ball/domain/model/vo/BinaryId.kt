package io.clroot.ball.domain.model.vo

import io.clroot.ball.domain.exception.InvalidIdException
import io.clroot.ball.domain.model.ValueObject

/**
 * ULID 기반의 바이너리 ID
 * 도메인 엔티티의 식별자로 사용되며, 타임스탬프 순서를 보장하고 전역적으로 고유함
 */
@JvmInline
value class BinaryId(
    private val value: String
) : ValueObject {

    companion object {
        /**
         * 새로운 BinaryId 생성
         */
        fun new(): BinaryId = BinaryId(ULIDSupport.generateULID())

        /**
         * 문자열로부터 BinaryId 생성
         * 
         * @param value ULID 문자열
         * @return BinaryId 인스턴스
         * @throws InvalidIdException 잘못된 ULID 형식인 경우
         */
        fun fromString(value: String): BinaryId {
            if (!ULIDSupport.isValidULID(value)) {
                throw InvalidIdException("Invalid ULID format: $value")
            }
            return BinaryId(value)
        }

        /**
         * 바이너리 데이터로부터 BinaryId 생성
         * 
         * @param bytes ULID 바이너리 데이터
         * @return BinaryId 인스턴스
         * @throws InvalidIdException 잘못된 바이너리 데이터인 경우
         */
        fun fromBytes(bytes: ByteArray): BinaryId {
            try {
                val ulid = ULIDSupport.bytesToULID(bytes)
                return BinaryId(ulid)
            } catch (e: Exception) {
                throw InvalidIdException("Invalid ULID bytes: ${e.message}")
            }
        }
    }

    /**
     * BinaryId를 바이너리 형태로 변환
     */
    fun toBytes(): ByteArray = ULIDSupport.ulidToBytes(value)

    override fun toString(): String = value
}
