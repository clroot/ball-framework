package io.clroot.ball.adapter.inbound.rest.controller

import arrow.core.left
import arrow.core.right
import io.clroot.ball.adapter.inbound.rest.extension.toResponseEntity
import io.clroot.ball.adapter.inbound.rest.extension.toResponseEntityWithNull
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.DomainException
import io.clroot.ball.domain.exception.DomainValidationException
import io.clroot.ball.domain.model.paging.PageRequest
import io.clroot.ball.domain.model.paging.Sort
import io.clroot.ball.domain.model.vo.BinaryId
import io.clroot.ball.domain.model.vo.Email
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 테스트 전용 컨트롤러
 *
 * REST 어댑터의 핵심 기능을 테스트하기 위한 간단한 컨트롤러입니다.
 * 실제 UseCase 대신 Mock 동작을 시뮬레이션합니다.
 */
@RestController
@RequestMapping("/api/test")
class TestController {
    /**
     * Either 성공 케이스 테스트
     */
    @GetMapping("/success")
    fun testSuccess(): ResponseEntity<TestResponse> {
        val response =
            TestResponse(
                id = BinaryId.new().toString(),
                message = "Success response",
                timestamp = LocalDateTime.now(),
            )
        return response.right().toResponseEntity()
    }

    /**
     * Either 실패 케이스 테스트 (도메인 에러)
     */
    @GetMapping("/domain-error")
    fun testDomainError(): ResponseEntity<TestResponse> {
        val error = ApplicationError.DomainError(DomainValidationException("Test validation error"))
        return error.left().toResponseEntity()
    }

    /**
     * Either 실패 케이스 테스트 (NotFound)
     */
    @GetMapping("/not-found")
    fun testNotFound(): ResponseEntity<TestResponse> = ApplicationError.NotFound.left().toResponseEntity()

    /**
     * Nullable 응답 테스트 (값 있음)
     */
    @GetMapping("/nullable/found")
    fun testNullableFound(): ResponseEntity<TestResponse> {
        val response =
            TestResponse(
                id = BinaryId.new().toString(),
                message = "Found response",
                timestamp = LocalDateTime.now(),
            )
        return response.right().toResponseEntityWithNull()
    }

    /**
     * Nullable 응답 테스트 (null)
     */
    @GetMapping("/nullable/not-found")
    fun testNullableNotFound(): ResponseEntity<TestResponse> = (null as TestResponse?).right().toResponseEntityWithNull()

    /**
     * 페이징 파라미터 테스트
     */
    @GetMapping("/paging")
    fun testPaging(
        @RequestParam(required = false, defaultValue = "0,20") page: PageRequest,
        @RequestParam(required = false, defaultValue = "") sort: Sort,
    ): ResponseEntity<PagedTestResponse> {
        val items =
            listOf(
                TestResponse(BinaryId.new().toString(), "Item 1", LocalDateTime.now()),
                TestResponse(BinaryId.new().toString(), "Item 2", LocalDateTime.now()),
            )

        val pagedResponse =
            PagedTestResponse(
                content = items,
                page = page.page,
                size = page.size,
                totalElements = 2L,
                sortInfo =
                    if (sort.isSorted) {
                        sort.orders.joinToString(",") { "${it.property}:${it.direction}" }
                    } else {
                        "unsorted"
                    },
            )

        return pagedResponse.right().toResponseEntity()
    }

    /**
     * Path Variable 테스트 (BinaryId)
     */
    @GetMapping("/items/{id}")
    fun testPathVariable(
        @PathVariable id: String,
    ): ResponseEntity<TestResponse> =
        try {
            val binaryId = BinaryId.fromString(id)
            val response =
                TestResponse(
                    id = binaryId.toString(),
                    message = "Found item with ID: $binaryId",
                    timestamp = LocalDateTime.now(),
                )
            response.right().toResponseEntity()
        } catch (e: Exception) {
            ApplicationError
                .DomainError(DomainValidationException.invalidId(id))
                .left()
                .toResponseEntity()
        }

    /**
     * POST 요청 테스트
     */
    @PostMapping("/items")
    fun testPostRequest(
        @RequestBody request: CreateTestRequest,
    ): ResponseEntity<TestResponse> =
        try {
            // 간단한 검증
            if (request.name.isBlank()) {
                throw DomainValidationException.fieldValidation("Name", "Name cannot be blank")
            }

            Email.from(request.email) // Email 검증

            val response =
                TestResponse(
                    id = BinaryId.new().toString(),
                    message = "Created: ${request.name}",
                    timestamp = LocalDateTime.now(),
                )
            response.right().toResponseEntity()
        } catch (e: Exception) {
            ApplicationError
                .DomainError(object : DomainException("", e) {})
                .left()
                .toResponseEntity()
        }

    /**
     * 시스템 에러 테스트
     */
    @GetMapping("/system-error")
    fun testSystemError(): ResponseEntity<TestResponse> {
        val systemError =
            ApplicationError.SystemError(
                message = "Test system error",
                cause = RuntimeException("Database connection failed"),
            )
        return systemError.left().toResponseEntity()
    }
}

/**
 * 테스트용 응답 DTO
 */
data class TestResponse(
    val id: String,
    val message: String,
    val timestamp: LocalDateTime,
)

/**
 * 테스트용 페이징 응답 DTO
 */
data class PagedTestResponse(
    val content: List<TestResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val sortInfo: String,
)

/**
 * 테스트용 생성 요청 DTO
 */
data class CreateTestRequest(
    val name: String,
    val email: String,
)
