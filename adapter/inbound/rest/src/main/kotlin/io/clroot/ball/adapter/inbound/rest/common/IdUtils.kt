package io.clroot.ball.adapter.inbound.rest.common

import io.clroot.ball.shared.core.exception.ValidationException
import java.util.UUID
import java.util.Base64

/**
 * ID 파라미터 처리 유틸리티
 * 
 * 이 클래스는 다양한 ID 타입과 문자열 ID 간의 변환 기능을 제공합니다.
 */
object IdUtils {

    /**
     * 문자열을 UUID로 변환
     * 
     * @param id UUID 형식의 문자열
     * @return UUID 객체
     * @throws ValidationException 유효하지 않은 UUID 형식인 경우
     */
    fun toUUID(id: String): UUID {
        return try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid UUID format: $id", e)
        }
    }

    /**
     * 문자열을 Long으로 변환
     * 
     * @param id Long 형식의 문자열
     * @return Long 값
     * @throws ValidationException 유효하지 않은 Long 형식인 경우
     */
    fun toLong(id: String): Long {
        return try {
            id.toLong()
        } catch (e: NumberFormatException) {
            throw ValidationException("Invalid Long format: $id", e)
        }
    }

    /**
     * 문자열을 Int로 변환
     * 
     * @param id Int 형식의 문자열
     * @return Int 값
     * @throws ValidationException 유효하지 않은 Int 형식인 경우
     */
    fun toInt(id: String): Int {
        return try {
            id.toInt()
        } catch (e: NumberFormatException) {
            throw ValidationException("Invalid Int format: $id", e)
        }
    }

    /**
     * Base64 인코딩된 문자열을 바이트 배열로 변환
     * 
     * @param id Base64 인코딩된 문자열
     * @return 바이트 배열
     * @throws ValidationException 유효하지 않은 Base64 형식인 경우
     */
    fun toBinary(id: String): ByteArray {
        return try {
            Base64.getDecoder().decode(id)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid Base64 format: $id", e)
        }
    }

    /**
     * 바이트 배열을 Base64 인코딩된 문자열로 변환
     * 
     * @param bytes 바이트 배열
     * @return Base64 인코딩된 문자열
     */
    fun fromBinary(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * BinaryId 클래스
     * 
     * 바이너리 데이터를 ID로 사용하기 위한 클래스입니다.
     */
    class BinaryId(val value: ByteArray) {
        
        constructor(base64: String) : this(toBinary(base64))
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as BinaryId
            
            return value.contentEquals(other.value)
        }
        
        override fun hashCode(): Int {
            return value.contentHashCode()
        }
        
        override fun toString(): String {
            return fromBinary(value)
        }
    }
}