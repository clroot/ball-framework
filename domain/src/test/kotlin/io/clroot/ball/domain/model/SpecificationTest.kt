package io.clroot.ball.domain.model

import io.clroot.ball.domain.exception.SpecificationNotSatisfiedException
import io.clroot.ball.domain.model.specification.Specification
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Instant

class SpecificationTest : FunSpec({

    // 테스트용 사용자 클래스
    data class User(
        override val id: String,
        val name: String,
        val age: Int,
        val email: String,
        val isActive: Boolean = true
    ) : EntityBase<String>(id, Instant.now(), Instant.now(), null)

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

    context("Single specification") {
        test("should satisfy specification") {
            // given
            val user = User("1", "John Doe", 25, "john@example.com")
            val adultSpec = AdultUserSpecification()

            // when & then
            adultSpec.isSatisfiedBy(user) shouldBe true
        }

        test("should not satisfy specification") {
            // given
            val user = User("1", "John Doe", 16, "john@example.com")
            val adultSpec = AdultUserSpecification()

            // when & then
            adultSpec.isSatisfiedBy(user) shouldBe false
        }
    }

    context("Combined specifications") {
        test("should combine specifications with AND") {
            // given
            val user1 = User("1", "John Doe", 25, "john@example.com", true)
            val user2 = User("2", "Jane Doe", 25, "jane@example.com", false)

            val adultSpec = AdultUserSpecification()
            val activeSpec = ActiveUserSpecification()
            val combinedSpec = adultSpec.and(activeSpec)

            // when & then
            combinedSpec.isSatisfiedBy(user1) shouldBe true
            combinedSpec.isSatisfiedBy(user2) shouldBe false
        }

        test("should combine specifications with OR") {
            // given
            val user1 = User("1", "John Doe", 25, "john@example.com", false)
            val user2 = User("2", "Jane Doe", 16, "jane@example.com", true)
            val user3 = User("3", "Bob Smith", 16, "bob@example.com", false)

            val adultSpec = AdultUserSpecification()
            val activeSpec = ActiveUserSpecification()
            val combinedSpec = adultSpec.or(activeSpec)

            // when & then
            combinedSpec.isSatisfiedBy(user1) shouldBe true
            combinedSpec.isSatisfiedBy(user2) shouldBe true
            combinedSpec.isSatisfiedBy(user3) shouldBe false
        }

        test("should negate specification with NOT") {
            // given
            val user1 = User("1", "John Doe", 25, "john@example.com")
            val user2 = User("2", "Jane Doe", 16, "jane@example.com")

            val adultSpec = AdultUserSpecification()
            val notAdultSpec = adultSpec.not()

            // when & then
            notAdultSpec.isSatisfiedBy(user1) shouldBe false
            notAdultSpec.isSatisfiedBy(user2) shouldBe true
        }
    }

    context("Validation") {
        test("should validate successfully and return original object") {
            // given
            val user = User("1", "John Doe", 25, "john@example.com")
            val adultSpec = AdultUserSpecification()
            val errorMessage = "User must be at least 18 years old"

            // when
            val result = adultSpec.validate(user, errorMessage)

            // then
            result shouldBe user
        }

        test("should throw SpecificationNotSatisfiedException for invalid object") {
            // given
            val user = User("2", "Jane Doe", 16, "jane@example.com")
            val adultSpec = AdultUserSpecification()
            val errorMessage = "User must be at least 18 years old"

            // when & then
            val exception = shouldThrow<SpecificationNotSatisfiedException> {
                adultSpec.validate(user, errorMessage)
            }
            exception.message shouldBe errorMessage
        }

        test("should validate with default error message") {
            // given
            val user = User("2", "Jane Doe", 16, "jane@example.com")
            val adultSpec = AdultUserSpecification()

            // when & then
            val exception = shouldThrow<SpecificationNotSatisfiedException> {
                adultSpec.validate(user)
            }
            exception.message shouldContain "Specification not satisfied for:"
        }
    }
})
