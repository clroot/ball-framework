package io.clroot.ball.user.application.service

import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

/**
 * 비밀번호 서비스 구현체 (Password Service Implementation)
 *
 * 비밀번호 해싱 및 검증을 위한 서비스 구현체
 */
@Service
class PasswordServiceImpl : PasswordService {

    private val secureRandom = SecureRandom()
    private val messageDigest = MessageDigest.getInstance("SHA-256")

    /**
     * 비밀번호 해싱
     *
     * @param password 평문 비밀번호
     * @return 해시된 비밀번호와 솔트를 포함한 결과
     */
    override fun hashPassword(password: String): PasswordHashResult {
        val salt = generateSalt()
        val hash = hashWithSalt(password, salt)

        return PasswordHashResult(hash, salt)
    }

    /**
     * 비밀번호 검증
     *
     * @param password 검증할 평문 비밀번호
     * @param hash 저장된 비밀번호 해시
     * @param salt 저장된 솔트
     * @return 비밀번호가 일치하면 true, 그렇지 않으면 false
     */
    override fun verifyPassword(password: String, hash: String, salt: String): Boolean {
        val computedHash = hashWithSalt(password, salt)
        return computedHash == hash
    }

    /**
     * 솔트 생성
     *
     * @return 생성된 솔트
     */
    private fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    /**
     * 비밀번호와 솔트를 사용하여 해시 생성
     *
     * @param password 평문 비밀번호
     * @param salt 솔트
     * @return 해시된 비밀번호
     */
    private fun hashWithSalt(password: String, salt: String): String {
        val saltedPassword = password + salt
        val hashBytes = messageDigest.digest(saltedPassword.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
