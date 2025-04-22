package io.clroot.ball.user.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class UserMetadataTest : FunSpec({
    context("UserMetadata.default") {
        test("should create UserMetadata with default values") {
            // When
            val metadata = UserMetadata.default()
            
            // Then
            metadata.displayName shouldBe null
            metadata.lastLoginAt shouldBe null
            metadata.passwordChangedAt shouldBe null
            metadata.loginFailCount shouldBe 0
            metadata.profileImageUrl shouldBe null
            metadata.emailVerified shouldBe false
        }
    }
    
    context("UserMetadata.updateLastLogin") {
        test("should update lastLoginAt and reset loginFailCount") {
            // Given
            val metadata = UserMetadata(
                displayName = "Test User",
                lastLoginAt = null,
                passwordChangedAt = null,
                loginFailCount = 3,
                profileImageUrl = null,
                emailVerified = false
            )
            val loginTime = Instant.now()
            
            // When
            val updatedMetadata = metadata.updateLastLogin(loginTime)
            
            // Then
            updatedMetadata.lastLoginAt shouldBe loginTime
            updatedMetadata.loginFailCount shouldBe 0
            
            // Other properties should remain unchanged
            updatedMetadata.displayName shouldBe metadata.displayName
            updatedMetadata.passwordChangedAt shouldBe metadata.passwordChangedAt
            updatedMetadata.profileImageUrl shouldBe metadata.profileImageUrl
            updatedMetadata.emailVerified shouldBe metadata.emailVerified
        }
    }
    
    context("UserMetadata.incrementLoginFailCount") {
        test("should increment loginFailCount") {
            // Given
            val initialCount = 2
            val metadata = UserMetadata(
                displayName = "Test User",
                lastLoginAt = null,
                passwordChangedAt = null,
                loginFailCount = initialCount,
                profileImageUrl = null,
                emailVerified = false
            )
            
            // When
            val updatedMetadata = metadata.incrementLoginFailCount()
            
            // Then
            updatedMetadata.loginFailCount shouldBe initialCount + 1
            
            // Other properties should remain unchanged
            updatedMetadata.displayName shouldBe metadata.displayName
            updatedMetadata.lastLoginAt shouldBe metadata.lastLoginAt
            updatedMetadata.passwordChangedAt shouldBe metadata.passwordChangedAt
            updatedMetadata.profileImageUrl shouldBe metadata.profileImageUrl
            updatedMetadata.emailVerified shouldBe metadata.emailVerified
        }
    }
    
    context("UserMetadata.updatePasswordChanged") {
        test("should update passwordChangedAt") {
            // Given
            val metadata = UserMetadata.default()
            val changeTime = Instant.now()
            
            // When
            val updatedMetadata = metadata.updatePasswordChanged(changeTime)
            
            // Then
            updatedMetadata.passwordChangedAt shouldBe changeTime
            
            // Other properties should remain unchanged
            updatedMetadata.displayName shouldBe metadata.displayName
            updatedMetadata.lastLoginAt shouldBe metadata.lastLoginAt
            updatedMetadata.loginFailCount shouldBe metadata.loginFailCount
            updatedMetadata.profileImageUrl shouldBe metadata.profileImageUrl
            updatedMetadata.emailVerified shouldBe metadata.emailVerified
        }
    }
    
    context("UserMetadata.updateEmailVerified") {
        test("should update emailVerified") {
            // Given
            val metadata = UserMetadata.default()
            
            // When
            val updatedMetadata = metadata.updateEmailVerified(true)
            
            // Then
            updatedMetadata.emailVerified shouldBe true
            
            // Other properties should remain unchanged
            updatedMetadata.displayName shouldBe metadata.displayName
            updatedMetadata.lastLoginAt shouldBe metadata.lastLoginAt
            updatedMetadata.passwordChangedAt shouldBe metadata.passwordChangedAt
            updatedMetadata.loginFailCount shouldBe metadata.loginFailCount
            updatedMetadata.profileImageUrl shouldBe metadata.profileImageUrl
        }
    }
})