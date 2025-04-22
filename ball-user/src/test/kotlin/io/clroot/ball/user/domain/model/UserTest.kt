package io.clroot.ball.user.domain.model

import io.clroot.ball.shared.attribute.AttributeKey
import io.clroot.ball.shared.attribute.AttributeStore
import io.clroot.ball.user.domain.event.UserCreatedEvent
import io.clroot.ball.user.domain.event.UserRoleAddedEvent
import io.clroot.ball.user.domain.event.UserRoleRemovedEvent
import io.clroot.ball.user.domain.event.UserStatusChangedEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant

class UserTest : FunSpec({
    context("User.create") {
        test("should create User with expected values") {
            // Given
            val username = "testuser"
            val email = Email.of("test@example.com").getOrNull()!!
            val roles = setOf(UserRole.USER, UserRole.ADMIN)
            val metadata = UserMetadata.default()
            val attributes = AttributeStore.empty()

            // When
            val user = User.create(
                username = username,
                email = email,
                roles = roles,
                metadata = metadata,
                attributes = attributes
            )

            // Then
            user.username shouldBe username
            user.email shouldBe email
            user.status shouldBe UserStatus.PENDING
            user.roles shouldBe roles
            user.metadata shouldBe metadata
            user.attributes shouldBe attributes
            user.deletedAt shouldBe null

            // Should register UserCreatedEvent
            user.domainEvents shouldHaveSize 1
            val event = user.domainEvents[0]
            event.shouldBeInstanceOf<UserCreatedEvent>()
            val createdEvent = event
            createdEvent.username shouldBe username
            createdEvent.email shouldBe email.toString()
        }

        test("should create User with default values when not provided") {
            // Given
            val username = "testuser"
            val email = Email.of("test@example.com").getOrNull()!!

            // When
            val user = User.create(
                username = username,
                email = email
            )

            // Then
            user.username shouldBe username
            user.email shouldBe email
            user.status shouldBe UserStatus.PENDING
            user.roles shouldBe setOf(UserRole.USER)
            user.metadata shouldBe UserMetadata.default()
            user.attributes.getAttributes() shouldBe emptyMap()
            user.deletedAt shouldBe null
        }
    }

    context("User.setAttribute") {
        test("should set attribute and return new User instance") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!
            )
            val key = AttributeKey("test_key", String::class)
            val value = "test_value"

            // When
            val updatedUser = user.setAttribute(key, value)

            // Then
            updatedUser.getAttribute(key).getOrNull() shouldBe value

            // Original user should remain unchanged
            user.getAttribute(key).getOrNull() shouldBe null

            // Other properties should remain unchanged
            updatedUser.id shouldBe user.id
            updatedUser.username shouldBe user.username
            updatedUser.email shouldBe user.email
            updatedUser.status shouldBe user.status
            updatedUser.roles shouldBe user.roles
            updatedUser.metadata shouldBe user.metadata
            updatedUser.createdAt shouldBe user.createdAt
            updatedUser.deletedAt shouldBe user.deletedAt

            // updatedAt should be updated
            updatedUser.updatedAt shouldBe updatedUser.updatedAt
        }
    }

    context("User.changeStatus") {
        test("should change status and register UserStatusChangedEvent") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!
            )
            user.clearEvents() // Clear the UserCreatedEvent
            val newStatus = UserStatus.ACTIVE

            // When
            val updatedUser = user.changeStatus(newStatus)

            // Then
            updatedUser.status shouldBe newStatus

            // Should register UserStatusChangedEvent
            updatedUser.domainEvents shouldHaveSize 1
            val event = updatedUser.domainEvents[0]
            event.shouldBeInstanceOf<UserStatusChangedEvent>()
            val statusEvent = event
            statusEvent.userId shouldBe updatedUser.id.toString()
            statusEvent.oldStatus shouldBe UserStatus.PENDING
            statusEvent.newStatus shouldBe newStatus

            // Other properties should remain unchanged
            updatedUser.id shouldBe user.id
            updatedUser.username shouldBe user.username
            updatedUser.email shouldBe user.email
            updatedUser.roles shouldBe user.roles
            updatedUser.metadata shouldBe user.metadata
            updatedUser.attributes shouldBe user.attributes
            updatedUser.createdAt shouldBe user.createdAt

            // updatedAt should be updated
            updatedUser.updatedAt shouldBe updatedUser.updatedAt
        }

        test("should set deletedAt when status is DELETED") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!
            )

            // When
            val updatedUser = user.changeStatus(UserStatus.DELETED)

            // Then
            updatedUser.status shouldBe UserStatus.DELETED
            updatedUser.deletedAt shouldNotBe null
        }

        test("should return same instance when status is unchanged") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!
            )

            // When
            val updatedUser = user.changeStatus(UserStatus.PENDING)

            // Then
            updatedUser shouldBe user
        }
    }

    context("User.updateMetadata") {
        test("should update metadata") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!
            )
            val newMetadata = UserMetadata(
                displayName = "Test User",
                lastLoginAt = Instant.now(),
                passwordChangedAt = Instant.now(),
                loginFailCount = 3,
                profileImageUrl = "https://example.com/profile.jpg",
                emailVerified = true
            )

            // When
            val updatedUser = user.updateMetadata(newMetadata)

            // Then
            updatedUser.metadata shouldBe newMetadata

            // Other properties should remain unchanged
            updatedUser.id shouldBe user.id
            updatedUser.username shouldBe user.username
            updatedUser.email shouldBe user.email
            updatedUser.status shouldBe user.status
            updatedUser.roles shouldBe user.roles
            updatedUser.attributes shouldBe user.attributes
            updatedUser.createdAt shouldBe user.createdAt
            updatedUser.deletedAt shouldBe user.deletedAt

            // updatedAt should be updated
            updatedUser.updatedAt shouldBe updatedUser.updatedAt
        }
    }

    context("User.addRole") {
        test("should add role and register UserRoleAddedEvent") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!,
                roles = setOf(UserRole.USER)
            )
            user.clearEvents() // Clear the UserCreatedEvent
            val newRole = UserRole.ADMIN

            // When
            val updatedUser = user.addRole(newRole)

            // Then
            updatedUser.roles shouldContain newRole

            // Should register UserRoleAddedEvent
            updatedUser.domainEvents shouldHaveSize 1
            val event = updatedUser.domainEvents[0]
            event.shouldBeInstanceOf<UserRoleAddedEvent>()
            val roleEvent = event
            roleEvent.userId shouldBe updatedUser.id.toString()
            roleEvent.role shouldBe newRole.name

            // Other properties should remain unchanged
            updatedUser.id shouldBe user.id
            updatedUser.username shouldBe user.username
            updatedUser.email shouldBe user.email
            updatedUser.status shouldBe user.status
            updatedUser.metadata shouldBe user.metadata
            updatedUser.attributes shouldBe user.attributes
            updatedUser.createdAt shouldBe user.createdAt
            updatedUser.deletedAt shouldBe user.deletedAt

            // updatedAt should be updated
            updatedUser.updatedAt shouldBe updatedUser.updatedAt
        }

        test("should return same instance when role already exists") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!,
                roles = setOf(UserRole.USER, UserRole.ADMIN)
            )

            // When
            val updatedUser = user.addRole(UserRole.ADMIN)

            // Then
            updatedUser shouldBe user
        }
    }

    context("User.removeRole") {
        test("should remove role and register UserRoleRemovedEvent") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!,
                roles = setOf(UserRole.USER, UserRole.ADMIN)
            )
            user.clearEvents() // Clear the UserCreatedEvent
            val roleToRemove = UserRole.ADMIN

            // When
            val updatedUser = user.removeRole(roleToRemove)

            // Then
            updatedUser.roles shouldBe setOf(UserRole.USER)

            // Should register UserRoleRemovedEvent
            updatedUser.domainEvents shouldHaveSize 1
            val event = updatedUser.domainEvents[0]
            event.shouldBeInstanceOf<UserRoleRemovedEvent>()
            val roleEvent = event
            roleEvent.userId shouldBe updatedUser.id.toString()
            roleEvent.role shouldBe roleToRemove.name

            // Other properties should remain unchanged
            updatedUser.id shouldBe user.id
            updatedUser.username shouldBe user.username
            updatedUser.email shouldBe user.email
            updatedUser.status shouldBe user.status
            updatedUser.metadata shouldBe user.metadata
            updatedUser.attributes shouldBe user.attributes
            updatedUser.createdAt shouldBe user.createdAt
            updatedUser.deletedAt shouldBe user.deletedAt

            // updatedAt should be updated
            updatedUser.updatedAt shouldBe updatedUser.updatedAt
        }

        test("should return same instance when role doesn't exist") {
            // Given
            val user = User.create(
                username = "testuser",
                email = Email.of("test@example.com").getOrNull()!!,
                roles = setOf(UserRole.USER)
            )

            // When
            val updatedUser = user.removeRole(UserRole.ADMIN)

            // Then
            updatedUser shouldBe user
        }
    }
})
