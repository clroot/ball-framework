package io.clroot.ball.domain.model.vo

import arrow.core.Either
import io.clroot.ball.domain.model.core.ValueObject
import io.clroot.ball.domain.model.vo.IdError.InvalidIdError

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
         */
        fun fromString(value: String): Either<IdError, BinaryId> =
            Either.catch {
                require(ULIDSupport.isValidULID(value)) { "Invalid ULID format: $value" }
                BinaryId(value)
            }.mapLeft { InvalidIdError }

        /**
         * 바이너리 데이터로부터 BinaryId 생성
         */
        fun fromBytes(bytes: ByteArray): Either<IdError, BinaryId> =
            Either.catch {
                val ulid = ULIDSupport.bytesToULID(bytes)
                BinaryId(ulid)
            }.mapLeft { InvalidIdError }
    }

    /**
     * BinaryId를 바이너리 형태로 변환
     */
    fun toBytes(): ByteArray = ULIDSupport.ulidToBytes(value)

    override fun toString(): String = value
}
