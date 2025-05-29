package io.clroot.ball.user.application.service

import arrow.core.Either
import io.clroot.ball.application.port.outbound.DomainEventPublisher
import io.clroot.ball.domain.event.DomainEvent
import io.clroot.ball.domain.port.PersistenceError
import io.clroot.ball.user.domain.model.Email
import io.clroot.ball.user.domain.model.User
import io.clroot.ball.user.domain.model.UserRole
import io.clroot.ball.user.domain.port.RegisterUserCommand
import io.clroot.ball.user.domain.port.UserError
import io.clroot.ball.user.domain.port.UserRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*

class UserServiceImplTest : FunSpec({
    context("register") {
        // Create mocks
        val userRepository = mockk<UserRepository>()
        val passwordService = mockk<PasswordService>()
        val domainEventPublisher = mockk<DomainEventPublisher>()

        // Create the service under test
        val userService = UserServiceImpl(userRepository, passwordService, domainEventPublisher)

        // Reset mocks before each test in this context
        beforeTest {
            clearMocks(
                userRepository,
                passwordService,
                domainEventPublisher,
                answers = true
            ) // answers = true 로 설정하여 Mock 응답 설정까지 초기화
        }

        test("should register a new user successfully") {
            // Given
            val username = "testuser"
            val email = "test@example.com"
            val password = "password123"
            val roles = setOf(UserRole.USER)

            val emailObj = Email.of(email).getOrNull()!!
            val passwordHash = "hashedPassword"
            val passwordSalt = "salt123"

            // Mock repository behavior
            every { userRepository.existsByUsername(username) } returns false
            every { userRepository.existsByEmail(emailObj) } returns false

            // Mock password service
            every { passwordService.hashPassword(password) } returns PasswordHashResult(passwordHash, passwordSalt)

            // Mock repository save
            val userSlot = slot<User>()
            every { userRepository.save(capture(userSlot)) } answers {
                Either.Right(userSlot.captured)
            }

            // Mock event publisher
            every { domainEventPublisher.publish(ofType<List<DomainEvent>>()) } returns Unit

            // Create command
            val command = RegisterUserCommand(
                username = username,
                email = email,
                password = password,
                roles = roles
            )

            // When
            val result = userService.register(command)

            // Then
            result.shouldBeRight()

            // Verify repository was called
            verify { userRepository.existsByUsername(username) }
            verify { userRepository.existsByEmail(emailObj) }
            verify { userRepository.save(any()) }

            // Verify password service was called
            verify { passwordService.hashPassword(password) }

            // Verify event publisher was called
            verify { domainEventPublisher.publish(any<List<DomainEvent>>()) }
        }

        test("should return InvalidEmail when email is invalid") {
            // Given
            val username = "testuser"
            val invalidEmail = "invalid-email"
            val password = "password123"

            // Create command
            val command = RegisterUserCommand(
                username = username,
                email = invalidEmail,
                password = password
            )

            // When
            val result = userService.register(command)

            // Then
            result.shouldBeLeft(UserError.InvalidEmail)

            // Verify no interactions with repository save
            verify(exactly = 0) { userRepository.save(any<User>()) }

            // Verify no interactions with event publisher
            verify(exactly = 0) { domainEventPublisher.publish(any<List<DomainEvent>>()) }
        }

        test("should return UsernameAlreadyExists when username exists") {
            // Given
            val username = "existinguser"
            val email = "test@example.com"
            val password = "password123"

            // Mock repository behavior
            every { userRepository.existsByUsername(username) } returns true

            // Create command
            val command = RegisterUserCommand(
                username = username,
                email = email,
                password = password
            )

            // When
            val result = userService.register(command)

            // Then
            result.shouldBeLeft(UserError.UsernameAlreadyExists)

            // Verify repository was called
            verify { userRepository.existsByUsername(username) }

            // Verify no interactions with repository save
            verify(exactly = 0) { userRepository.save(any()) }

            // Verify no interactions with event publisher
            verify(exactly = 0) { domainEventPublisher.publish(any<List<DomainEvent>>()) }
        }

        test("should return EmailAlreadyExists when email exists") {
            // Given
            val username = "testuser"
            val email = "existing@example.com"
            val password = "password123"

            val emailObj = Email.of(email).getOrNull()!!

            // Mock repository behavior
            every { userRepository.existsByUsername(username) } returns false
            every { userRepository.existsByEmail(emailObj) } returns true

            // Create command
            val command = RegisterUserCommand(
                username = username,
                email = email,
                password = password
            )

            // When
            val result = userService.register(command)

            // Then
            result.shouldBeLeft(UserError.EmailAlreadyExists)

            // Verify repository was called
            verify { userRepository.existsByUsername(username) }
            verify { userRepository.existsByEmail(emailObj) }

            // Verify no interactions with repository save
            verify(exactly = 0) { userRepository.save(any()) }

            // Verify no interactions with event publisher
            verify(exactly = 0) { domainEventPublisher.publish(any<List<DomainEvent>>()) }
        }

        test("should return SystemError when repository save fails") {
            // Given
            val username = "testuser"
            val email = "test@example.com"
            val password = "password123"

            val emailObj = Email.of(email).getOrNull()!!
            val passwordHash = "hashedPassword"
            val passwordSalt = "salt123"

            // Mock repository behavior
            every { userRepository.existsByUsername(username) } returns false
            every { userRepository.existsByEmail(emailObj) } returns false

            // Mock password service
            every { passwordService.hashPassword(password) } returns PasswordHashResult(passwordHash, passwordSalt)

            // Mock repository save to fail
            every { userRepository.save(any()) } returns Either.Left(PersistenceError.DatabaseError(Exception("Database error")))

            // Create command
            val command = RegisterUserCommand(
                username = username,
                email = email,
                password = password
            )

            // When
            val result = userService.register(command)

            // Then
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error.shouldBeInstanceOf<UserError.SystemError>()

            // Verify repository was called
            verify { userRepository.existsByUsername(username) }
            verify { userRepository.existsByEmail(emailObj) }
            verify { userRepository.save(any()) }

            // Verify password service was called
            verify { passwordService.hashPassword(password) }

            // Verify no interactions with event publisher
            verify(exactly = 0) { domainEventPublisher.publish(any<List<DomainEvent>>()) }
        }
    }
})
