package io.clroot.ball.domain.model.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class SpecificationTest {

    // 테스트용 사용자 클래스
    data class User(
        override val id: String,
        val name: String,
        val age: Int,
        val email: String,
        val isActive: Boolean = true
    ) : EntityBase<String>(id)

    // 테스트용 명세 구현
    class AdultUserSpecification : Specification<User> {
        override fun isSatisfiedBy(t: User): Boolean = t.age >= 18
    }

    class ActiveUserSpecification : Specification<User> {
        override fun isSatisfiedBy(t: User): Boolean = t.isActive
    }

    class ValidEmailSpecification : Specification<User> {
        override fun isSatisfiedBy(t: User): Boolean {
            val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            return t.email.matches(emailRegex.toRegex()) && !t.email.contains(" ")
        }
    }

    @Test
    fun `should satisfy single specification`() {
        // given
        val user = User("1", "John Doe", 25, "john@example.com")
        val adultSpec = AdultUserSpecification()

        // when & then
        assertTrue(adultSpec.isSatisfiedBy(user))
    }

    @Test
    fun `should not satisfy single specification`() {
        // given
        val user = User("1", "John Doe", 16, "john@example.com")
        val adultSpec = AdultUserSpecification()

        // when & then
        assertFalse(adultSpec.isSatisfiedBy(user))
    }

    @Test
    fun `should combine specifications with AND`() {
        // given
        val user1 = User("1", "John Doe", 25, "john@example.com", true)
        val user2 = User("2", "Jane Doe", 25, "jane@example.com", false)

        val adultSpec = AdultUserSpecification()
        val activeSpec = ActiveUserSpecification()
        val combinedSpec = adultSpec.and(activeSpec)

        // when & then
        assertTrue(combinedSpec.isSatisfiedBy(user1))
        assertFalse(combinedSpec.isSatisfiedBy(user2))
    }

    @Test
    fun `should combine specifications with OR`() {
        // given
        val user1 = User("1", "John Doe", 25, "john@example.com", false)
        val user2 = User("2", "Jane Doe", 16, "jane@example.com", true)
        val user3 = User("3", "Bob Smith", 16, "bob@example.com", false)

        val adultSpec = AdultUserSpecification()
        val activeSpec = ActiveUserSpecification()
        val combinedSpec = adultSpec.or(activeSpec)

        // when & then
        assertTrue(combinedSpec.isSatisfiedBy(user1))
        assertTrue(combinedSpec.isSatisfiedBy(user2))
        assertFalse(combinedSpec.isSatisfiedBy(user3))
    }

    @Test
    fun `should negate specification with NOT`() {
        // given
        val user1 = User("1", "John Doe", 25, "john@example.com")
        val user2 = User("2", "Jane Doe", 16, "jane@example.com")

        val adultSpec = AdultUserSpecification()
        val notAdultSpec = adultSpec.not()

        // when & then
        assertFalse(notAdultSpec.isSatisfiedBy(user1))
        assertTrue(notAdultSpec.isSatisfiedBy(user2))
    }

    @Test
    fun `should validate and return Either`() {
        // given
        val user1 = User("1", "John Doe", 25, "john@example.com")
        val user2 = User("2", "Jane Doe", 16, "jane@example.com")

        val adultSpec = AdultUserSpecification()
        val error = "User must be at least 18 years old"

        // when & then
        val result1 = adultSpec.validate(user1, error)
        assertTrue(result1.isRight())

        val result2 = adultSpec.validate(user2, error)
        assertTrue(result2.isLeft())
        result2.mapLeft { errorMsg ->
            assertEquals(error, errorMsg)
        }
    }

    private fun assertEquals(expected: String, actual: String) {
        assertTrue(expected == actual)
    }
}
