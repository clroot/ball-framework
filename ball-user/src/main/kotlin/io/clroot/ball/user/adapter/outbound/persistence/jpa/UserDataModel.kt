package io.clroot.ball.user.adapter.outbound.persistence.jpa

import io.clroot.ball.domain.model.core.BinaryId
import io.clroot.ball.shared.attribute.AttributeStore
import io.clroot.ball.user.domain.model.*
import jakarta.persistence.*
import java.time.Instant

/**
 * 사용자 데이터 모델 (User Data Model)
 *
 * 사용자 도메인 모델의 JPA 엔티티 구현
 */
@Entity
@Table(name = "users")
class UserDataModel {
    @Id
    @Column(name = "id", columnDefinition = "binary(16)", nullable = false)
    var id: BinaryId? = null

    @Column(name = "username", length = 50, nullable = false, unique = true)
    lateinit var username: String

    @Column(name = "email", length = 100, nullable = false, unique = true)
    lateinit var email: String

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    lateinit var status: UserStatus

    @Column(name = "roles", length = 200, nullable = false)
    lateinit var roles: String // 쉼표로 구분된 역할 목록

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: Instant

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    // 메타데이터 필드
    @Column(name = "display_name", length = 100)
    var displayName: String? = null

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null

    @Column(name = "password_changed_at")
    var passwordChangedAt: Instant? = null

    @Column(name = "login_fail_count", nullable = false)
    var loginFailCount: Int = 0

    @Column(name = "profile_image_url", length = 255)
    var profileImageUrl: String? = null

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false

    // JSON 형식으로 저장된 속성
    @Column(name = "attributes", columnDefinition = "text")
    var attributes: String? = null

    // JPA를 위한 기본 생성자
    protected constructor()

    // 모든 필드를 초기화하는 생성자
    constructor(
        id: BinaryId,
        username: String,
        email: String,
        status: UserStatus,
        roles: String,
        createdAt: Instant,
        updatedAt: Instant,
        deletedAt: Instant?,
        displayName: String?,
        lastLoginAt: Instant?,
        passwordChangedAt: Instant?,
        loginFailCount: Int,
        profileImageUrl: String?,
        emailVerified: Boolean,
        attributes: String? = null
    ) {
        this.id = id
        this.username = username
        this.email = email
        this.status = status
        this.roles = roles
        this.createdAt = createdAt
        this.updatedAt = updatedAt
        this.deletedAt = deletedAt
        this.displayName = displayName
        this.lastLoginAt = lastLoginAt
        this.passwordChangedAt = passwordChangedAt
        this.loginFailCount = loginFailCount
        this.profileImageUrl = profileImageUrl
        this.emailVerified = emailVerified
        this.attributes = attributes
    }

    /**
     * 데이터 모델을 도메인 모델로 변환
     *
     * @return 도메인 모델
     */
    fun toDomain(): User {
        val userRoles = roles.split(",")
            .filter { it.isNotBlank() }
            .map { UserRole.valueOf(it) }
            .toSet()

        val metadata = UserMetadata(
            displayName = displayName,
            lastLoginAt = lastLoginAt,
            passwordChangedAt = passwordChangedAt,
            loginFailCount = loginFailCount,
            profileImageUrl = profileImageUrl,
            emailVerified = emailVerified
        )

        // 속성은 AttributePersistenceProvider 에서 처리
        return User.from(
            id = id!!,
            username = username,
            email = Email.of(email).getOrNull()!!,
            status = status,
            roles = userRoles,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            metadata = metadata,
            attributes = AttributeStore.empty()
        )
    }

    companion object {
        /**
         * 도메인 모델을 데이터 모델로 변환
         *
         * @param user 도메인 모델
         * @return 데이터 모델
         */
        fun fromDomain(user: User): UserDataModel {
            return UserDataModel(
                id = user.id,
                username = user.username,
                email = user.email.value,
                status = user.status,
                roles = user.roles.joinToString(",") { it.name },
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                deletedAt = user.deletedAt,
                displayName = user.metadata.displayName,
                lastLoginAt = user.metadata.lastLoginAt,
                passwordChangedAt = user.metadata.passwordChangedAt,
                loginFailCount = user.metadata.loginFailCount,
                profileImageUrl = user.metadata.profileImageUrl,
                emailVerified = user.metadata.emailVerified
                // attributes는 AttributePersistenceProvider에서 처리
            )
        }
    }
}