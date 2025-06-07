package io.clroot.ball.adapter.inbound.rest.dto.error

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.Instant

class ErrorResponseTest : DescribeSpec({

    val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }

    describe("ErrorResponse") {
        
        describe("기본 생성") {
            
            it("필수 필드만으로 생성할 수 있어야 한다") {
                // given
                val code = "VALIDATION_FAILED"
                val message = "Validation error"
                
                // when
                val errorResponse = ErrorResponse(code, message)
                
                // then
                errorResponse.code shouldBe code
                errorResponse.message shouldBe message
                errorResponse.timestamp shouldNotBe null
                errorResponse.traceId shouldBe null
                errorResponse.details shouldBe null
                errorResponse.debug shouldBe null
            }
            
            it("모든 필드를 포함해서 생성할 수 있어야 한다") {
                // given
                val code = "BUSINESS_RULE_VIOLATION"
                val message = "Business rule violated"
                val timestamp = Instant.now()
                val traceId = "trace-123"
                val details = mapOf("field" to "error")
                val debug = DebugInfo(path = "/test", method = "POST")
                
                // when
                val errorResponse = ErrorResponse(
                    code = code,
                    message = message,
                    timestamp = timestamp,
                    traceId = traceId,
                    details = details,
                    debug = debug
                )
                
                // then
                errorResponse.code shouldBe code
                errorResponse.message shouldBe message
                errorResponse.timestamp shouldBe timestamp
                errorResponse.traceId shouldBe traceId
                errorResponse.details shouldBe details
                errorResponse.debug shouldBe debug
            }
        }
        
        describe("JSON 직렬화") {
            
            it("모든 필드가 있는 경우 JSON으로 직렬화되어야 한다") {
                // given
                val errorResponse = ErrorResponse(
                    code = "TEST_ERROR",
                    message = "Test error message",
                    traceId = "trace-123",
                    details = mapOf("field1" to "error1", "field2" to "error2"),
                    debug = DebugInfo(
                        path = "/api/test",
                        method = "POST",
                        exceptionType = "ValidationException"
                    )
                )
                
                // when
                val json = objectMapper.writeValueAsString(errorResponse)
                
                // then
                json shouldContain "\"code\":\"TEST_ERROR\""
                json shouldContain "\"message\":\"Test error message\""
                json shouldContain "\"traceId\":\"trace-123\""
                json shouldContain "\"details\""
                json shouldContain "\"debug\""
                json shouldContain "\"timestamp\""
            }
            
            it("null 필드는 JSON에서 제외되어야 한다") {
                // given
                val errorResponse = ErrorResponse(
                    code = "SIMPLE_ERROR",
                    message = "Simple error"
                )
                
                // when
                val json = objectMapper.writeValueAsString(errorResponse)
                
                // then
                json shouldContain "\"code\":\"SIMPLE_ERROR\""
                json shouldContain "\"message\":\"Simple error\""
                json shouldContain "\"timestamp\""
                json shouldNotContain "\"traceId\""
                json shouldNotContain "\"details\""
                json shouldNotContain "\"debug\""
            }
            
            it("빈 details 맵도 제외되어야 한다") {
                // given
                val errorResponse = ErrorResponse(
                    code = "ERROR",
                    message = "Error message",
                    details = emptyMap()
                )
                
                // when
                val json = objectMapper.writeValueAsString(errorResponse)
                
                // then
                json shouldNotContain "\"details\""
            }
        }
        
        describe("JSON 역직렬화") {
            
            it("JSON에서 ErrorResponse로 역직렬화할 수 있어야 한다") {
                // given
                val json = """
                {
                    "code": "DESERIALIZATION_TEST",
                    "message": "Test message",
                    "timestamp": "2023-01-01T00:00:00Z",
                    "traceId": "trace-456",
                    "details": {
                        "field": "error"
                    }
                }
                """.trimIndent()
                
                // when
                val errorResponse = objectMapper.readValue(json, ErrorResponse::class.java)
                
                // then
                errorResponse.code shouldBe "DESERIALIZATION_TEST"
                errorResponse.message shouldBe "Test message"
                errorResponse.traceId shouldBe "trace-456"
                errorResponse.details shouldBe mapOf("field" to "error")
            }
            
            it("필수 필드만 있는 JSON도 역직렬화할 수 있어야 한다") {
                // given
                val json = """
                {
                    "code": "MINIMAL_ERROR",
                    "message": "Minimal error",
                    "timestamp": "2023-01-01T00:00:00Z"
                }
                """.trimIndent()
                
                // when
                val errorResponse = objectMapper.readValue(json, ErrorResponse::class.java)
                
                // then
                errorResponse.code shouldBe "MINIMAL_ERROR"
                errorResponse.message shouldBe "Minimal error"
                errorResponse.traceId shouldBe null
                errorResponse.details shouldBe null
                errorResponse.debug shouldBe null
            }
        }
        
        describe("데이터 클래스 기능") {
            
            it("copy 함수가 올바르게 동작해야 한다") {
                // given
                val original = ErrorResponse(
                    code = "ORIGINAL",
                    message = "Original message",
                    traceId = "trace-123"
                )
                
                // when
                val copied = original.copy(message = "Updated message")
                
                // then
                copied.code shouldBe original.code
                copied.message shouldBe "Updated message"
                copied.traceId shouldBe original.traceId
                copied.timestamp shouldBe original.timestamp
            }
            
            it("equality가 올바르게 동작해야 한다") {
                // given
                val timestamp = Instant.now()
                val errorResponse1 = ErrorResponse("CODE", "Message", timestamp)
                val errorResponse2 = ErrorResponse("CODE", "Message", timestamp)
                val errorResponse3 = ErrorResponse("CODE", "Different Message", timestamp)
                
                // then
                (errorResponse1 == errorResponse2) shouldBe true
                (errorResponse1 == errorResponse3) shouldBe false
            }
        }
        
        describe("복합 시나리오") {
            
            it("복잡한 details와 debug 정보가 포함된 에러 응답을 처리할 수 있어야 한다") {
                // given
                val details = mapOf(
                    "validation_errors" to mapOf(
                        "email" to "Invalid format",
                        "age" to "Must be positive"
                    ),
                    "business_errors" to listOf("Rule 1 violated", "Rule 2 violated")
                )
                
                val debug = DebugInfo(
                    path = "/api/users",
                    method = "POST",
                    exceptionType = "ComplexValidationException",
                    stackTrace = "Full stack trace...",
                    location = "UserController.createUser:45"
                )
                
                val errorResponse = ErrorResponse(
                    code = "COMPLEX_VALIDATION_FAILED",
                    message = "Multiple validation errors occurred",
                    traceId = "complex-trace-789",
                    details = details,
                    debug = debug
                )
                
                // when
                val json = objectMapper.writeValueAsString(errorResponse)
                val deserialized = objectMapper.readValue(json, ErrorResponse::class.java)
                
                // then
                deserialized.code shouldBe "COMPLEX_VALIDATION_FAILED"
                deserialized.message shouldBe "Multiple validation errors occurred"
                deserialized.traceId shouldBe "complex-trace-789"
                deserialized.details shouldNotBe null
                deserialized.debug shouldNotBe null
            }
        }
    }
})
