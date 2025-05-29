package io.clroot.ball.user.domain.model

import io.clroot.ball.domain.model.core.AggregateRoot
import io.clroot.ball.domain.model.vo.BinaryId
import io.clroot.ball.shared.attribute.Attributable
import io.clroot.ball.shared.attribute.AttributeKey
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
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    version: Long,
    val username: String,
    val email: Email,
    val status: UserStatus,
    val roles: Set<UserRole>,
    val metadata: UserMetadata,
    override val attributes: AttributeStore
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt, version), Attributable<User> {
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
        @JvmStatic
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
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                version = 0L,
                username = username,
                email = email,
                status = UserStatus.PENDING,
                roles = roles,
                metadata = metadata,
                attributes = attributes
            )

            user.registerEvent(UserCreatedEvent(user.id.toString(), username, email.value))
            return user
        }

        @JvmStatic
        fun from(
            id: BinaryId,
            createdAt: Instant,
            updatedAt: Instant,
            deletedAt: Instant?,
            version: Long,
            username: String,
            email: Email,
            status: UserStatus,
            roles: Set<UserRole>,
            metadata: UserMetadata,
            attributes: AttributeStore
        ): User = User(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            version = version,
            username = username,
            email = email,
            status = status,
            roles = roles,
            metadata = metadata,
            attributes = attributes
        )
    }

    /**
     * 사용자 속성 설정
     *
     * @param key 속성 키
     * @param value 속성 값
     * @return 업데이트된 User 객체
     */
    override fun <V : Any> setAttribute(key: AttributeKey<V>, value: V): User {
        return copy(
            updatedAt = Instant.now(),
            attributes = attributes.setAttribute(key, value)
        )
    }

    /**
     * 사용자 속성 일괄 설정 (주의: 안전하지 않음)
     *
     * 주어진 AttributeStore로 사용자의 모든 속성을 직접 교체합니다.
     * 이 메서드는 `attributes` 필드를 업데이트하고 `updatedAt` 필드를 현재 시간으로 설정합니다.
     * 기존 속성을 완전히 덮어쓰므로 주의해서 사용해야 합니다.
     *
     * @param attributes 사용자에게 설정할 속성을 담은 AttributeStore
     * @return 업데이트된 User 객체
     */

    override fun unsafeSetAttributes(attributes: AttributeStore): User {
        return copy(
            updatedAt = Instant.now(),
            attributes = attributes,
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

        val user = copy(
            updatedAt = Instant.now(),
            deletedAt = if (newStatus == UserStatus.DELETED) Instant.now() else deletedAt,
            status = newStatus,
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
        return copy(
            updatedAt = Instant.now(),
            metadata = newMetadata,
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

        val user = copy(
            roles = roles + role,
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

        val user = copy(
            roles = roles - role,
        )

        user.registerEvent(UserRoleRemovedEvent(user.id.toString(), role.name))
        return user
    }

    private fun copy(
        createdAt: Instant = this.createdAt,
        updatedAt: Instant = this.updatedAt,
        deletedAt: Instant? = this.deletedAt,
        version: Long = this.version,
        username: String = this.username,
        email: Email = this.email,
        status: UserStatus = this.status,
        roles: Set<UserRole> = this.roles,
        metadata: UserMetadata = this.metadata,
        attributes: AttributeStore = this.attributes
    ): User = User(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        version = version,
        username = username,
        email = email,
        status = status,
        roles = roles,
        metadata = metadata,
        attributes = attributes
    )
}
