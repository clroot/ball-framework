package io.clroot.ball.user.adapter.outbound.persistence.jpa

import io.clroot.ball.adapter.outbound.persistence.jpa.record.VersionedBinaryIdRecord
import io.clroot.ball.domain.model.vo.BinaryId
import io.clroot.ball.shared.attribute.AttributeStore
import io.clroot.ball.user.domain.model.*
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

/**
 * 사용자 데이터 모델 (User Data Model)
 *
 * 사용자 도메인 모델의 JPA 엔티티 구현
 */
@Entity
@Table(name = "users")
class UserRecord(
    id: BinaryId,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    version: Long,
    username: String,
    email: String,
    status: UserStatus,
    roles: String,
    displayName: String?,
    lastLoginAt: Instant?,
    passwordChangedAt: Instant?,
    loginFailCount: Int,
    profileImageUrl: String?,
    emailVerified: Boolean,
    attributes: String? = null
) : VersionedBinaryIdRecord<User>(id, createdAt, updatedAt, deletedAt, version) {
    var username: String = username
        protected set

    var email: String = email
        protected set

    var status: UserStatus = status
        protected set

    var roles: String = roles
        protected set

    var displayName: String? = displayName
        protected set

    var lastLoginAt: Instant? = lastLoginAt
        protected set

    var passwordChangedAt: Instant? = passwordChangedAt
        protected set

    var loginFailCount: Int = loginFailCount
        protected set

    var profileImageUrl: String? = profileImageUrl
        protected set

    var emailVerified: Boolean = emailVerified
        protected set

    var attributes: String? = attributes
        protected set


    constructor(user: User) : this(
        id = user.id,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt,
        deletedAt = user.deletedAt,
        version = user.version,
        username = user.username,
        email = user.email.value,
        status = user.status,
        roles = user.roles.joinToString(",") { it.name },
        displayName = user.metadata.displayName,
        lastLoginAt = user.metadata.lastLoginAt,
        passwordChangedAt = user.metadata.passwordChangedAt,
        loginFailCount = user.metadata.loginFailCount,
        profileImageUrl = user.metadata.profileImageUrl,
        emailVerified = user.metadata.emailVerified,
        attributes = null,
    )

    override fun toDomain(): User {
        return User.from(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            version = version,
            username = username,
            email = Email.of(email).getOrNull()!!,
            status = status,
            roles = roles.split(",").map { UserRole.valueOf(it) }.toSet(),
            metadata = UserMetadata(
                displayName = displayName,
                lastLoginAt = lastLoginAt,
                passwordChangedAt = passwordChangedAt,
                loginFailCount = loginFailCount,
                profileImageUrl = profileImageUrl,
                emailVerified = emailVerified
            ),
            attributes = AttributeStore.empty()
        )
    }

    override fun update(entity: User) {
        this.username = entity.username
        this.email = entity.email.value
        this.status = entity.status
        this.roles = entity.roles.joinToString(",") { it.name }
        this.displayName = entity.metadata.displayName
        this.lastLoginAt = entity.metadata.lastLoginAt
        this.passwordChangedAt = entity.metadata.passwordChangedAt
        this.loginFailCount = entity.metadata.loginFailCount
        this.profileImageUrl = entity.metadata.profileImageUrl
        this.emailVerified = entity.metadata.emailVerified
        this.attributes = null
    }
}