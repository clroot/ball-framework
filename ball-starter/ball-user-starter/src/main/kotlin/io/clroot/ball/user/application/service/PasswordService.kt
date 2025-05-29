package io.clroot.ball.user.application.service

/**
 * 비밀번호 서비스 (Password Service)
 *
 * 비밀번호 해싱 및 검증을 위한 서비스 인터페이스
 */
interface PasswordService {
    /**
     * 비밀번호 해싱
     *
     * @param password 평문 비밀번호
     * @return 해시된 비밀번호와 솔트를 포함한 결과
     */
    fun hashPassword(password: String): PasswordHashResult

    /**
     * 비밀번호 검증
     *
     * @param password 검증할 평문 비밀번호
     * @param hash 저장된 비밀번호 해시
     * @param salt 저장된 솔트
     * @return 비밀번호가 일치하면 true, 그렇지 않으면 false
     */
    fun verifyPassword(password: String, hash: String, salt: String): Boolean
}

/**
 * 비밀번호 해시 결과 (Password Hash Result)
 *
 * 비밀번호 해싱 결과를 담는 데이터 클래스
 *
 * @param hash 해시된 비밀번호
 * @param salt 사용된 솔트
 */
data class PasswordHashResult(
    val hash: String,
    val salt: String
)