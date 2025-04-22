package io.clroot.ball.user.adapter.inbound

import io.clroot.ball.domain.model.core.BinaryId
import io.clroot.ball.user.domain.model.User
import io.clroot.ball.user.domain.model.UserStatus
import io.clroot.ball.user.domain.port.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 사용자 컨트롤러 (User Controller)
 *
 * 사용자 관리를 위한 REST API 엔드포인트를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    /**
     * 사용자 등록
     *
     * @param request 사용자 등록 요청 DTO
     * @return 등록된 사용자 정보
     */
    @PostMapping
    fun registerUser(@RequestBody request: RegisterUserRequest): ResponseEntity<Any> {
        return userService.register(
            RegisterUserCommand(
                username = request.username,
                email = request.email,
                password = request.password,
                roles = request.roles?.toSet() ?: emptySet()
            )
        ).fold(
            { handleUserError(it) },
            { ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(it)) }
        )
    }

    /**
     * 사용자 인증 (로그인)
     *
     * @param request 로그인 요청 DTO
     * @return 인증된 사용자 정보
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        return userService.authenticate(
            AuthenticateUserCommand(
                usernameOrEmail = request.usernameOrEmail,
                password = request.password
            )
        ).fold(
            { handleUserError(it) },
            { ResponseEntity.ok(UserResponse.fromUser(it)) }
        )
    }

    /**
     * 사용자 정보 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: String): ResponseEntity<Any> {
        return BinaryId.fromString(userId).fold(
            { ResponseEntity.badRequest().body(ErrorResponse("Invalid user ID format")) },
            { id ->
                userService.getUser(id).fold(
                    { handleUserError(it) },
                    { ResponseEntity.ok(UserResponse.fromUser(it)) }
                )
            }
        )
    }

    /**
     * 사용자 이름으로 사용자 조회
     *
     * @param username 사용자 이름
     * @return 사용자 정보
     */
    @GetMapping("/by-username/{username}")
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<Any> {
        return userService.getUserByUsername(username).fold(
            { handleUserError(it) },
            { ResponseEntity.ok(UserResponse.fromUser(it)) }
        )
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자 정보
     */
    @GetMapping("/by-email/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<Any> {
        return userService.getUserByEmail(email).fold(
            { handleUserError(it) },
            { ResponseEntity.ok(UserResponse.fromUser(it)) }
        )
    }

    /**
     * 비밀번호 변경
     *
     * @param userId 사용자 ID
     * @param request 비밀번호 변경 요청 DTO
     * @return 성공 시 204 No Content
     */
    @PutMapping("/{userId}/password")
    fun changePassword(
        @PathVariable userId: String,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Any> {
        return userService.changePassword(
            ChangePasswordCommand(
                userId = userId,
                currentPassword = request.currentPassword,
                newPassword = request.newPassword
            )
        ).fold(
            { handleUserError(it) },
            { ResponseEntity.noContent().build() }
        )
    }

    /**
     * 사용자 상태 변경
     *
     * @param userId 사용자 ID
     * @param request 상태 변경 요청 DTO
     * @return 업데이트된 사용자 정보
     */
    @PutMapping("/{userId}/status")
    fun changeStatus(
        @PathVariable userId: String,
        @RequestBody request: ChangeStatusRequest
    ): ResponseEntity<Any> {
        return BinaryId.fromString(userId).fold(
            { ResponseEntity.badRequest().body(ErrorResponse("Invalid user ID format")) },
            { id ->
                try {
                    val status = UserStatus.valueOf(request.status.uppercase())
                    userService.changeStatus(id, status).fold(
                        { handleUserError(it) },
                        { ResponseEntity.ok(UserResponse.fromUser(it)) }
                    )
                } catch (e: IllegalArgumentException) {
                    ResponseEntity.badRequest().body(ErrorResponse("Invalid status: ${request.status}"))
                }
            }
        )
    }

    /**
     * 사용자 오류 처리
     *
     * @param error 사용자 오류
     * @return 적절한 HTTP 응답
     */
    private fun handleUserError(error: UserError): ResponseEntity<Any> {
        return when (error) {
            is UserError.UserNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse("User not found"))

            is UserError.UsernameAlreadyExists ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse("Username already exists"))

            is UserError.EmailAlreadyExists ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse("Email already exists"))

            is UserError.InvalidEmail ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse("Invalid email format"))

            is UserError.InvalidPassword ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse("Invalid password"))

            is UserError.AuthenticationFailed ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse("Authentication failed"))

            is UserError.UserNotActive ->
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse("User is not active"))

            is UserError.UserSuspended ->
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse("User is suspended"))

            is UserError.UserDeleted ->
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse("User is deleted"))

            is UserError.PermissionDenied ->
                ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse("Permission denied"))

            is UserError.SystemError ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse("System error: ${error.message}"))
        }
    }
}

/**
 * 사용자 등록 요청 DTO
 */
data class RegisterUserRequest(
    val username: String,
    val email: String,
    val password: String,
    val roles: List<io.clroot.ball.user.domain.model.UserRole>? = null
)

/**
 * 로그인 요청 DTO
 */
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

/**
 * 비밀번호 변경 요청 DTO
 */
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

/**
 * 상태 변경 요청 DTO
 */
data class ChangeStatusRequest(
    val status: String
)

/**
 * 사용자 응답 DTO
 */
data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val status: String,
    val roles: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val metadata: UserMetadataResponse
) {
    companion object {
        fun fromUser(user: User): UserResponse {
            return UserResponse(
                id = user.id.toString(),
                username = user.username,
                email = user.email.value,
                status = user.status.name,
                roles = user.roles.map { it.name },
                createdAt = user.createdAt.toString(),
                updatedAt = user.updatedAt.toString(),
                metadata = UserMetadataResponse(
                    displayName = user.metadata.displayName,
                    lastLoginAt = user.metadata.lastLoginAt?.toString(),
                    passwordChangedAt = user.metadata.passwordChangedAt?.toString(),
                    loginFailCount = user.metadata.loginFailCount,
                    profileImageUrl = user.metadata.profileImageUrl,
                    emailVerified = user.metadata.emailVerified
                )
            )
        }
    }
}

/**
 * 사용자 메타데이터 응답 DTO
 */
data class UserMetadataResponse(
    val displayName: String?,
    val lastLoginAt: String?,
    val passwordChangedAt: String?,
    val loginFailCount: Int,
    val profileImageUrl: String?,
    val emailVerified: Boolean
)

/**
 * 오류 응답 DTO
 */
data class ErrorResponse(
    val message: String
)
