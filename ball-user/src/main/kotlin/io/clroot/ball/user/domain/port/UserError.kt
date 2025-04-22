package io.clroot.ball.user.domain.port

/**
 * 사용자 오류 (User Error)
 *
 * 사용자 모듈에서 발생할 수 있는 오류 유형을 정의하는 봉인 클래스
 */
sealed class UserError {
    /**
     * 사용자를 찾을 수 없음
     */
    data object UserNotFound : UserError()

    /**
     * 사용자 이름이 이미 존재함
     */
    data object UsernameAlreadyExists : UserError()

    /**
     * 이메일이 이미 존재함
     */
    data object EmailAlreadyExists : UserError()

    /**
     * 잘못된 이메일 형식
     */
    data object InvalidEmail : UserError()

    /**
     * 잘못된 비밀번호
     */
    data object InvalidPassword : UserError()

    /**
     * 인증 실패
     */
    data object AuthenticationFailed : UserError()

    /**
     * 사용자가 활성화되지 않음
     */
    data object UserNotActive : UserError()

    /**
     * 사용자가 일시 정지됨
     */
    data object UserSuspended : UserError()

    /**
     * 사용자가 삭제됨
     */
    data object UserDeleted : UserError()

    /**
     * 권한 없음
     */
    data object PermissionDenied : UserError()

    /**
     * 시스템 오류
     */
    data class SystemError(val message: String, val cause: Throwable? = null) : UserError()
}