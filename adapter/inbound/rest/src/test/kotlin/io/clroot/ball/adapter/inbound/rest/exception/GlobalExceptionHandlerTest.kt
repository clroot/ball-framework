package io.clroot.ball.adapter.inbound.rest.exception

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
