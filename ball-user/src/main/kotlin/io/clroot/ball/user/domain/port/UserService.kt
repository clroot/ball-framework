package io.clroot.ball.user.domain.port

import arrow.core.Either
import io.clroot.ball.domain.model.core.BinaryId
import io.clroot.ball.user.domain.model.User
import io.clroot.ball.user.domain.model.UserStatus

/**
 * 사용자 서비스 (User Service)
 *
 * 사용자 관리를 위한 도메인 서비스 인터페이스
 */
interface UserService {
    /**
     * 사용자 등록
     *
     * @param command 사용자 등록 명령
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    fun register(command: RegisterUserCommand): Either<UserError, User>

    /**
     * 사용자 인증
     *
     * @param command 사용자 인증 명령
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    fun authenticate(command: AuthenticateUserCommand): Either<UserError, User>

    /**
     * 비밀번호 변경
     *
     * @param command 비밀번호 변경 명령
     * @return 성공 시 Unit, 실패 시 UserError
     */
    fun changePassword(command: ChangePasswordCommand): Either<UserError, Unit>

    /**
     * 사용자 상태 변경
     *
     * @param userId 사용자 ID
     * @param newStatus 새로운 상태
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    fun changeStatus(userId: BinaryId, newStatus: UserStatus): Either<UserError, User>

    /**
     * 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    fun getUser(userId: BinaryId): Either<UserError, User>

    /**
     * 사용자 이름으로 사용자 조회
     *
     * @param username 사용자 이름
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    fun getUserByUsername(username: String): Either<UserError, User>

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    fun getUserByEmail(email: String): Either<UserError, User>
}