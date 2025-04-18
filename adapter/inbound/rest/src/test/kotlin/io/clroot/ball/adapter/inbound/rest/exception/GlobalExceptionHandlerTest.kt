package io.clroot.ball.adapter.inbound.rest.exception

import io.clroot.ball.shared.core.exception.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

class GlobalExceptionHandlerTest : FunSpec({

    val handler = GlobalExceptionHandler()
    val webRequest = mockk<WebRequest>()

    beforeTest {
        // Mock the WebRequest behavior
        every { webRequest.getDescription(false) } returns "uri=/api/test"
    }

    test("should handle AuthenticationException") {
        // Given
        val exception = AuthenticationException("Authentication failed")

        // When
        val response = handler.handleAuthenticationException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.body?.status shouldBe HttpStatus.UNAUTHORIZED.value()
        response.body?.error shouldBe HttpStatus.UNAUTHORIZED.reasonPhrase
        response.body?.message shouldBe "Authentication failed"
        response.body?.path shouldBe "/api/test"
    }

    test("should handle AuthorizationException") {
        // Given
        val exception = AuthorizationException("Authorization failed")

        // When
        val response = handler.handleAuthorizationException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.FORBIDDEN
        response.body?.status shouldBe HttpStatus.FORBIDDEN.value()
        response.body?.error shouldBe HttpStatus.FORBIDDEN.reasonPhrase
        response.body?.message shouldBe "Authorization failed"
        response.body?.path shouldBe "/api/test"
    }

    test("should handle ValidationException") {
        // Given
        val errors = mapOf("email" to "Invalid email format", "password" to "Password too short")
        val exception = ValidationException("Validation failed", null, errors)

        // When
        val response = handler.handleValidationException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.status shouldBe HttpStatus.BAD_REQUEST.value()
        response.body?.error shouldBe HttpStatus.BAD_REQUEST.reasonPhrase
        response.body?.message shouldBe "Validation failed"
        response.body?.path shouldBe "/api/test"
        response.body?.errors shouldBe errors
    }

    test("should handle EntityNotFoundException") {
        // Given
        val exception = EntityNotFoundException("User", 123)

        // When
        val response = handler.handleEntityNotFoundException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.NOT_FOUND
        response.body?.status shouldBe HttpStatus.NOT_FOUND.value()
        response.body?.error shouldBe HttpStatus.NOT_FOUND.reasonPhrase
        response.body?.message shouldBe "User with id 123 not found"
        response.body?.path shouldBe "/api/test"
    }

    test("should handle DuplicateEntityException") {
        // Given
        val exception = DuplicateEntityException("User", "email", "test@example.com")

        // When
        val response = handler.handleDuplicateEntityException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.CONFLICT
        response.body?.status shouldBe HttpStatus.CONFLICT.value()
        response.body?.error shouldBe HttpStatus.CONFLICT.reasonPhrase
        response.body?.message shouldBe "User with email test@example.com already exists"
        response.body?.path shouldBe "/api/test"
    }

    test("should handle BusinessRuleViolationException") {
        // Given
        val exception = BusinessRuleViolationException("Business rule violated")

        // When
        val response = handler.handleBusinessRuleViolationException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.UNPROCESSABLE_ENTITY
        response.body?.status shouldBe HttpStatus.UNPROCESSABLE_ENTITY.value()
        response.body?.error shouldBe HttpStatus.UNPROCESSABLE_ENTITY.reasonPhrase
        response.body?.message shouldBe "Business rule violated"
        response.body?.path shouldBe "/api/test"
    }

    test("should handle ExternalServiceException") {
        // Given
        val exception = ExternalServiceException(serviceName = "PaymentService", message = "External service failed")

        // When
        val response = handler.handleExternalServiceException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        response.body?.status shouldBe HttpStatus.SERVICE_UNAVAILABLE.value()
        response.body?.error shouldBe HttpStatus.SERVICE_UNAVAILABLE.reasonPhrase
        response.body?.message shouldBe "PaymentService error: External service failed"
        response.body?.path shouldBe "/api/test"
    }

    test("should handle MethodArgumentNotValidException") {
        // Given
        val bindingResult = mockk<BindingResult>()
        val fieldErrors = listOf(
            FieldError("user", "email", "Invalid email format"),
            FieldError("user", "password", "Password too short")
        )
        every { bindingResult.fieldErrors } returns fieldErrors

        val exception = mockk<MethodArgumentNotValidException>()
        every { exception.bindingResult } returns bindingResult
        every { exception.message } returns "Validation failed"

        // When
        val response = handler.handleMethodArgumentNotValidException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.status shouldBe HttpStatus.BAD_REQUEST.value()
        response.body?.error shouldBe HttpStatus.BAD_REQUEST.reasonPhrase
        response.body?.message shouldBe "Validation failed"
        response.body?.path shouldBe "/api/test"
        response.body?.errors shouldBe mapOf(
            "email" to "Invalid email format",
            "password" to "Password too short"
        )
    }

    test("should handle MethodArgumentTypeMismatchException") {
        // Given
        val exception = mockk<MethodArgumentTypeMismatchException>()
        every { exception.name } returns "userId"
        every { exception.message } returns "Failed to convert value of type 'String' to required type 'Long'"

        // When
        val response = handler.handleMethodArgumentTypeMismatchException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.status shouldBe HttpStatus.BAD_REQUEST.value()
        response.body?.error shouldBe HttpStatus.BAD_REQUEST.reasonPhrase
        response.body?.message shouldBe "Type mismatch for parameter 'userId': Failed to convert value of type 'String' to required type 'Long'"
        response.body?.path shouldBe "/api/test"
    }

    test("should handle generic Exception") {
        // Given
        val exception = RuntimeException("Unexpected error")

        // When
        val response = handler.handleException(exception, webRequest)

        // Then
        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        response.body?.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.body?.error shouldBe HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase
        response.body?.message shouldBe "An unexpected error occurred: Unexpected error"
        response.body?.path shouldBe "/api/test"
    }
})
