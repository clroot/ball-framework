package io.clroot.ball.domain.model

import io.clroot.ball.domain.exception.DomainValidationException
import io.clroot.ball.domain.model.vo.Email
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class EmailTest : FunSpec({
    
    context("Email creation") {
        test("should create valid email") {
            // given
            val validEmail = "test@example.com"
            
            // when
            val email = Email.from(validEmail)
            
            // then
            email.value shouldBe validEmail
        }
        
        test("should reject invalid email formats") {
            // given
            val invalidEmails = listOf(
                "invalid-email",
                "missing@tld",
                "@missing-local-part.com",
                "spaces in@email.com",
                ""
            )
            
            // when & then
            invalidEmails.forEach { invalidEmail ->
                val exception = shouldThrow<DomainValidationException> {
                    Email.from(invalidEmail)
                }
                exception.message shouldContain "Invalid email format"
            }
        }
        
        test("should convert to string correctly") {
            // given
            val emailStr = "test@example.com"
            
            // when
            val email = Email.from(emailStr)
            
            // then
            email.toString() shouldBe emailStr
        }
        
        test("should handle complex valid email formats") {
            // given
            val validComplexEmails = listOf(
                "user.name+tag@example.com",
                "user123@sub.domain.co.uk",
                "test-email@example-domain.org"
            )
            
            // when & then
            validComplexEmails.forEach { validEmail ->
                val email = Email.from(validEmail)
                email.value shouldBe validEmail
            }
        }
    }
})
