package io.clroot.ball.user.domain.model

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EmailTest : FunSpec({
    context("Email.of") {
        test("should create Email for valid email address") {
            // Given
            val validEmail = "test@example.com"

            // When
            val result = Email.of(validEmail)

            // Then
            result.shouldBeRight()
            result.getOrNull()!!.toString() shouldBe validEmail
        }

        test("should return InvalidEmailError for invalid email address") {
            // Given
            val invalidEmails = listOf(
                "",                  // Empty string
                "test",              // No @ symbol
                "test@",             // No domain
                "@example.com",      // No local part
                "test@example",      // No TLD
                "test@.com",         // No domain name
                "test@example.",     // TLD is empty
                "test@exam ple.com", // Space in domain
                "te st@example.com"  // Space in local part
            )

            // When & Then
            invalidEmails.forEach { invalidEmail ->
                val result = Email.of(invalidEmail)
                result.shouldBeLeft(InvalidEmailError)
            }
        }
    }

    context("Email.toString") {
        test("should return the original email string") {
            // Given
            val email = "test@example.com"
            val emailObj = Email.of(email).getOrNull()!!

            // When
            val result = emailObj.toString()

            // Then
            result shouldBe email
        }
    }
})
