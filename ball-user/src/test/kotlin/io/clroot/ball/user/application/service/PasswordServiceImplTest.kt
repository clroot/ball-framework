package io.clroot.ball.user.application.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty

class PasswordServiceImplTest : FunSpec({
    val passwordService = PasswordServiceImpl()

    context("hashPassword") {
        test("should return PasswordHashResult with non-empty hash and salt") {
            // Given
            val password = "securePassword123"

            // When
            val result = passwordService.hashPassword(password)

            // Then
            result.hash.shouldNotBeEmpty()
            result.salt.shouldNotBeEmpty()
        }

        test("should return different hash for same password with different salt") {
            // Given
            val password = "securePassword123"

            // When
            val result1 = passwordService.hashPassword(password)
            val result2 = passwordService.hashPassword(password)

            // Then
            result1.hash shouldNotBe result2.hash
            result1.salt shouldNotBe result2.salt
        }
    }

    context("verifyPassword") {
        test("should return true for correct password") {
            // Given
            val password = "securePassword123"
            val hashResult = passwordService.hashPassword(password)

            // When
            val isValid = passwordService.verifyPassword(password, hashResult.hash, hashResult.salt)

            // Then
            isValid shouldBe true
        }

        test("should return false for incorrect password") {
            // Given
            val correctPassword = "securePassword123"
            val incorrectPassword = "wrongPassword456"
            val hashResult = passwordService.hashPassword(correctPassword)

            // When
            val isValid = passwordService.verifyPassword(incorrectPassword, hashResult.hash, hashResult.salt)

            // Then
            isValid shouldBe false
        }

        test("should return false for correct password with wrong salt") {
            // Given
            val password = "securePassword123"
            val hashResult1 = passwordService.hashPassword(password)
            val hashResult2 = passwordService.hashPassword(password)

            // When
            val isValid = passwordService.verifyPassword(password, hashResult1.hash, hashResult2.salt)

            // Then
            isValid shouldBe false
        }

        test("should return false for correct password with wrong hash") {
            // Given
            val password = "securePassword123"
            val hashResult1 = passwordService.hashPassword(password)
            val hashResult2 = passwordService.hashPassword(password)

            // When
            val isValid = passwordService.verifyPassword(password, hashResult2.hash, hashResult1.salt)

            // Then
            isValid shouldBe false
        }
    }
})