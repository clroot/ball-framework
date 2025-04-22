package io.clroot.ball.user.domain.model

import io.clroot.ball.domain.model.core.AggregateRoot
import io.clroot.ball.domain.model.core.BinaryId
import io.clroot.ball.shared.attribute.Attributable
import io.clroot.ball.shared.attribute.AttributeStore
import io.clroot.ball.user.domain.event.UserCreatedEvent
import io.clroot.ball.user.domain.event.UserRoleAddedEvent
import io.clroot.ball.user.domain.event.UserRoleRemovedEvent
import io.clroot.ball.user.domain.event.UserStatusChangedEvent
import java.time.Instant

/**
 * 사용자 (User)
 *
 * 사용자 정보를 나타내는 집합체 루트 엔티티
 */
class User private constructor(
    id: BinaryId,
    val username: String,
    val email: Email,
    val status: UserStatus,
    val roles: Set<UserRole>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val metadata: UserMetadata,
    override val attributes: AttributeStore
) : AggregateRoot<BinaryId>(id), Attributable<User> {

    /**
     * 사용자 속성 설정
     *
     * @param key 속성 키
     * @param value 속성 값
     * @return 업데이트된 User 객체
     */
    override fun <V : Any> setAttribute(key: io.clroot.ball.shared.attribute.AttributeKey<V>, value: V): User {
        return User(
            id = id,
            username = username,
            email = email,
            status = status,
            roles = roles,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = deletedAt,
            metadata = metadata,
            attributes = attributes.setAttribute(key, value)
        )
    }

    /**
     * 사용자 상태 변경
     *
     * @param newStatus 새로운 상태
     * @return 업데이트된 User 객체
     */
    fun changeStatus(newStatus: UserStatus): User {
        if (status == newStatus) return this

        val user = User(
            id = id,
            username = username,
            email = email,
            status = newStatus,
            roles = roles,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = if (newStatus == UserStatus.DELETED) Instant.now() else deletedAt,
            metadata = metadata,
            attributes = attributes
        )

        user.registerEvent(UserStatusChangedEvent(user.id.toString(), status, newStatus))
        return user
    }

    /**
     * 사용자 메타데이터 업데이트
     *
     * @param newMetadata 새로운 메타데이터
     * @return 업데이트된 User 객체
     */
    fun updateMetadata(newMetadata: UserMetadata): User {
        return User(
            id = id,
            username = username,
            email = email,
            status = status,
            roles = roles,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = deletedAt,
            metadata = newMetadata,
            attributes = attributes
        )
    }

    /**
     * 사용자 역할 추가
     *
     * @param role 추가할 역할
     * @return 업데이트된 User 객체
     */
    fun addRole(role: UserRole): User {
        if (roles.contains(role)) return this

        val user = User(
            id = id,
            username = username,
            email = email,
            status = status,
            roles = roles + role,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = deletedAt,
            metadata = metadata,
            attributes = attributes
        )

        user.registerEvent(UserRoleAddedEvent(user.id.toString(), role.name))
        return user
    }

    /**
     * 사용자 역할 제거
     *
     * @param role 제거할 역할
     * @return 업데이트된 User 객체
     */
    fun removeRole(role: UserRole): User {
        if (!roles.contains(role)) return this

        val user = User(
            id = id,
            username = username,
            email = email,
            status = status,
            roles = roles - role,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = deletedAt,
            metadata = metadata,
            attributes = attributes
        )

        user.registerEvent(UserRoleRemovedEvent(user.id.toString(), role.name))
        return user
    }

    companion object {
        /**
         * 새로운 사용자 생성
         *
         * @param username 사용자 이름
         * @param email 이메일
         * @param roles 역할 집합
         * @param metadata 메타데이터
         * @param attributes 속성 저장소
         * @return 생성된 User 객체
         */
        fun create(
            username: String,
            email: Email,
            roles: Set<UserRole> = setOf(UserRole.USER),
            metadata: UserMetadata = UserMetadata.default(),
            attributes: AttributeStore = AttributeStore.empty()
        ): User {
            val now = Instant.now()
            val id = BinaryId.new()

            val user = User(
                id = id,
                username = username,
                email = email,
                status = UserStatus.PENDING,
                roles = roles,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                metadata = metadata,
                attributes = attributes
            )

            user.registerEvent(UserCreatedEvent(user.id.toString(), username, email.value))
            return user
        }
    }
}
