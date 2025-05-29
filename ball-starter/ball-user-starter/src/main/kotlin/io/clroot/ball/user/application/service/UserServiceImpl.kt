package io.clroot.ball.user.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.model.vo.BinaryId
import io.clroot.ball.user.domain.model.Email
import io.clroot.ball.user.domain.model.User
import io.clroot.ball.user.domain.model.UserAttributeKeys
import io.clroot.ball.user.domain.model.UserStatus
import io.clroot.ball.user.domain.port.*
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 사용자 서비스 구현체 (User Service Implementation)
 *
 * 사용자 관리를 위한 도메인 서비스 구현체
 */
@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordService: PasswordService,
    private val domainEventPublisher: DomainEventPublisher
) : UserService {

    /**
     * 사용자 등록
     *
     * @param command 사용자 등록 명령
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    override fun register(command: RegisterUserCommand): Either<UserError, User> {
        // 이메일 유효성 검사
        val emailResult = Email.of(command.email)
        if (emailResult.isLeft()) {
            return UserError.InvalidEmail.left()
        }

        val email = emailResult.getOrNull()!!

        // 사용자 이름 중복 검사
        if (userRepository.existsByUsername(command.username)) {
            return UserError.UsernameAlreadyExists.left()
        }

        // 이메일 중복 검사
        if (userRepository.existsByEmail(email)) {
            return UserError.EmailAlreadyExists.left()
        }

        // 비밀번호 해싱
        val passwordResult = passwordService.hashPassword(command.password)

        // 사용자 생성
        val user = User.create(
            username = command.username,
            email = email,
            roles = command.roles,
            metadata = command.metadata,
            attributes = command.attributes
                .setAttribute(UserAttributeKeys.PASSWORD_HASH, passwordResult.hash)
                .setAttribute(UserAttributeKeys.PASSWORD_SALT, passwordResult.salt)
        )

        // 사용자 저장
        return userRepository.save(user).mapLeft {
            UserError.SystemError("Failed to save user")
        }.map { savedUser ->
            // 도메인 이벤트 발행
            domainEventPublisher.publish(savedUser.domainEvents)
            savedUser.clearEvents()
            savedUser
        }
    }

    /**
     * 사용자 인증
     *
     * @param command 사용자 인증 명령
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    override fun authenticate(command: AuthenticateUserCommand): Either<UserError, User> {
        // 사용자 이름 또는 이메일로 사용자 조회
        val user = if (command.usernameOrEmail.contains("@")) {
            getUserByEmail(command.usernameOrEmail)
        } else {
            getUserByUsername(command.usernameOrEmail)
        }

        return user.flatMap { foundUser ->
            // 사용자 상태 확인
            when (foundUser.status) {
                UserStatus.PENDING -> UserError.UserNotActive.left()
                UserStatus.SUSPENDED -> UserError.UserSuspended.left()
                UserStatus.INACTIVE -> UserError.UserNotActive.left()
                UserStatus.DELETED -> UserError.UserDeleted.left()
                UserStatus.ACTIVE -> {
                    // 비밀번호 검증
                    val passwordHash = foundUser.getAttribute(UserAttributeKeys.PASSWORD_HASH)
                    val passwordSalt = foundUser.getAttribute(UserAttributeKeys.PASSWORD_SALT)

                    if (passwordHash.isNone() || passwordSalt.isNone()) {
                        return@flatMap UserError.AuthenticationFailed.left()
                    }

                    val isValid = passwordService.verifyPassword(
                        command.password,
                        passwordHash.getOrNull()!!,
                        passwordSalt.getOrNull()!!
                    )

                    if (!isValid) {
                        // 로그인 실패 횟수 증가
                        val updatedUser = foundUser.updateMetadata(
                            foundUser.metadata.incrementLoginFailCount()
                        )
                        userRepository.save(updatedUser)
                        return@flatMap UserError.AuthenticationFailed.left()
                    }

                    // 로그인 성공 시 마지막 로그인 시간 업데이트
                    val updatedUser = foundUser.updateMetadata(
                        foundUser.metadata.updateLastLogin(Instant.now())
                    )
                    userRepository.save(updatedUser).mapLeft {
                        UserError.SystemError("Failed to update user", it as Exception)
                    }.map { it }
                }
            }
        }
    }

    /**
     * 비밀번호 변경
     *
     * @param command 비밀번호 변경 명령
     * @return 성공 시 Unit, 실패 시 UserError
     */
    override fun changePassword(command: ChangePasswordCommand): Either<UserError, Unit> {
        // 사용자 ID로 사용자 조회
        return BinaryId.fromString(command.userId).fold(
            { UserError.UserNotFound.left() },
            { id -> getUser(id) }
        ).flatMap { user ->
            // 현재 비밀번호 검증
            val passwordHash = user.getAttribute(UserAttributeKeys.PASSWORD_HASH)
            val passwordSalt = user.getAttribute(UserAttributeKeys.PASSWORD_SALT)

            if (passwordHash.isNone() || passwordSalt.isNone()) {
                return@flatMap UserError.AuthenticationFailed.left()
            }

            val isValid = passwordService.verifyPassword(
                command.currentPassword,
                passwordHash.getOrNull()!!,
                passwordSalt.getOrNull()!!
            )

            if (!isValid) {
                return@flatMap UserError.InvalidPassword.left()
            }

            // 새 비밀번호 해싱
            val passwordResult = passwordService.hashPassword(command.newPassword)

            // 비밀번호 업데이트
            val updatedUser = user
                .setAttribute(UserAttributeKeys.PASSWORD_HASH, passwordResult.hash)
                .setAttribute(UserAttributeKeys.PASSWORD_SALT, passwordResult.salt)
                .updateMetadata(user.metadata.updatePasswordChanged(Instant.now()))

            userRepository.save(updatedUser).mapLeft {
                UserError.SystemError("Failed to update user", it as Exception)
            }.map { }
        }
    }

    /**
     * 사용자 상태 변경
     *
     * @param userId 사용자 ID
     * @param newStatus 새로운 상태
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    override fun changeStatus(userId: BinaryId, newStatus: UserStatus): Either<UserError, User> {
        return getUser(userId).flatMap { user ->
            val updatedUser = user.changeStatus(newStatus)

            userRepository.save(updatedUser).mapLeft {
                UserError.SystemError("Failed to update user status", it as Exception)
            }.map { savedUser ->
                // 도메인 이벤트 발행
                domainEventPublisher.publish(savedUser.domainEvents)
                savedUser.clearEvents()
                savedUser
            }
        }
    }

    /**
     * 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    override fun getUser(userId: BinaryId): Either<UserError, User> {
        return userRepository.findById(userId).toEither { UserError.UserNotFound }
    }

    /**
     * 사용자 이름으로 사용자 조회
     *
     * @param username 사용자 이름
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    override fun getUserByUsername(username: String): Either<UserError, User> {
        return userRepository.findByUsername(username).toEither { UserError.UserNotFound }
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 성공 시 User 객체, 실패 시 UserError
     */
    override fun getUserByEmail(email: String): Either<UserError, User> {
        val emailResult = Email.of(email)
        if (emailResult.isLeft()) {
            return UserError.InvalidEmail.left()
        }

        return userRepository.findByEmail(emailResult.getOrNull()!!).toEither { UserError.UserNotFound }
    }
}
