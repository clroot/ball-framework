package io.clroot.ball.domain.model.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmailTest {
    
    @Test
    fun `should create valid email`() {
        // given
        val validEmail = "test@example.com"
        
        // when
        val result = Email.from(validEmail)
        
        // then
        assertTrue(result.isRight())
        result.map { email ->
            assertEquals(validEmail, email.value)
        }
    }
    
    @Test
    fun `should reject invalid email`() {
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
            val result = Email.from(invalidEmail)
            assertTrue(result.isLeft())
            result.mapLeft { error ->
                assertTrue(error.message.contains("Invalid email format"))
            }
        }
    }
    
    @Test
    fun `should convert to string`() {
        // given
        val emailStr = "test@example.com"
        
        // when
        val email = Email.from(emailStr).getOrNull()
        
        // then
        assertEquals(emailStr, email.toString())
    }
}