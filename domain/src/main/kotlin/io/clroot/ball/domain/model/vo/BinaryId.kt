package io.clroot.ball.domain.model.vo

import io.clroot.ball.domain.model.ValueObject
import java.nio.ByteBuffer
import java.util.*

/**
 * ULID 기반의 바이너리 ID
 * 도메인 엔티티의 식별자로 사용되며, 타임스탬프 순서를 보장하고 전역적으로 고유함
 */
@JvmInline
value class BinaryId private constructor(
    val value: String,
) : ValueObject {
    companion object {
        /**
         * 새로운 BinaryId 생성
         */
        @JvmStatic
        fun generate(): BinaryId = BinaryId(ULIDSupport.generateULID())

        /**
         * 문자열로부터 BinaryId 생성
         *
         * @param value ULID 문자열
         * @return BinaryId 인스턴스
         * @throws DomainValidationException 잘못된 ULID 형식인 경우
         */
        @JvmStatic
        fun of(value: String): BinaryId {
            if (!ULIDSupport.isValidULID(value)) {
                throw IllegalArgumentException("Invalid ULID format: $value")
            }
            return BinaryId(value)
        }

        /**
         * 바이너리 데이터로부터 BinaryId 생성
         *
         * @param bytes ULID 바이너리 데이터
         * @return BinaryId 인스턴스
         * @throws DomainValidationException 잘못된 바이너리 데이터인 경우
         */
        @JvmStatic
        fun of(bytes: ByteArray): BinaryId {
            try {
                val ulid = ULIDSupport.bytesToULID(bytes)
                return BinaryId(ulid)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid ULID bytes: ${e.message}")
            }
        }

        /**
         * UUID로부터 BinaryId 생성
         *
         * 주의: 이 메서드는 UUID의 바이트를 ULID 형식으로 인코딩합니다.
         * ULID와 UUID는 서로 다른 형식이므로:
         * - BinaryId.of(uuid).uuid는 원본 uuid와 다를 수 있습니다
         * - 동일한 UUID는 항상 동일한 BinaryId를 생성합니다
         * - BinaryId의 주 목적은 ULID이며, UUID 지원은 제한적입니다
         *
         * UUID의 완벽한 보존이 필요한 경우 별도의 UUID 필드를 사용하세요.
         *
         * @param uuid 변환할 UUID
         * @return BinaryId 인스턴스
         */
        @JvmStatic
        fun of(uuid: UUID): BinaryId {
            val buffer = ByteBuffer.allocate(16)
            buffer.putLong(uuid.mostSignificantBits)
            buffer.putLong(uuid.leastSignificantBits)
            return of(buffer.array())
        }
    }

    /**
     * BinaryId를 바이너리 형태로 변환
     */
    val bytes: ByteArray get() = ULIDSupport.ulidToBytes(value)

    /**
     * BinaryId를 UUID로 변환
     *
     * BinaryId의 바이트 표현을 UUID로 변환합니다.
     */
    val uuid: UUID
        get() {
            val buffer = ByteBuffer.wrap(bytes)
            val mostSigBits = buffer.long
            val leastSigBits = buffer.long
            return UUID(mostSigBits, leastSigBits)
        }

    override fun toString(): String = value
}
