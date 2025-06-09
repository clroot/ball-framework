package io.clroot.ball.adapter.inbound.rest.exception

import io.clroot.ball.adapter.outbound.data.access.core.exception.DuplicateEntityException
import io.clroot.ball.adapter.outbound.data.access.core.exception.EntityNotFoundException
import io.clroot.ball.adapter.outbound.data.access.core.exception.PersistenceException
import io.clroot.ball.domain.exception.BusinessRuleException
import io.clroot.ball.domain.exception.DomainValidationException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest : DescribeSpec({

    val environment = mockk<Environment>()
    val request = mockk<HttpServletRequest>()
    val handler = GlobalExceptionHandler(environment)

    beforeEach {
        every { request.requestURI } returns "/api/v1/users"
        every { request.method } returns "POST"
        every { environment.activeProfiles } returns arrayOf("test")
    }

    describe("handleDomainException") {
        
        context("DomainValidationException 처리") {
            it("DomainValidationException을 400 Bad Request로 변환해야 한다") {
                // given
                val exception = DomainValidationException("Invalid user data")
                
                // when
                val response = handler.handleDomainException(exception, request)
                
                // then
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                response.body?.code shouldBe ErrorCodes.VALIDATION_FAILED
                response.body?.message shouldBe "Invalid user data"
                response.body?.traceId shouldNotBe null
            }
        }
        
        context("BusinessRuleException 처리") {
            it("BusinessRuleException을 400 Bad Request로 변환해야 한다") {
                // given
                val exception = BusinessRuleException("Cannot delete user with active orders")
                
                // when
                val response = handler.handleDomainException(exception, request)
                
                // then
                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                response.body?.code shouldBe ErrorCodes.BUSINESS_RULE_VIOLATION
                response.body?.message shouldBe "Cannot delete user with active orders"
            }
        }
    }
    
    describe("handlePersistenceException") {
        
        context("EntityNotFoundException 처리") {
            it("EntityNotFoundException을 404 Not Found로 변환해야 한다") {
                // given
                val exception = EntityNotFoundException("User with ID 123 not found")
                
                // when
                val response = handler.handlePersistenceException(exception, request)
                
                // then
                response.statusCode shouldBe HttpStatus.NOT_FOUND
                response.body?.code shouldBe ErrorCodes.NOT_FOUND
                response.body?.message shouldBe "User with ID 123 not found"
            }
        }
        
        context("DuplicateEntityException 처리") {
            it("DuplicateEntityException을 409 Conflict로 변환해야 한다") {
                // given
                val exception = DuplicateEntityException("User with email already exists")
                
                // when
                val response = handler.handlePersistenceException(exception, request)
                
                // then
                response.statusCode shouldBe HttpStatus.CONFLICT
                response.body?.code shouldBe ErrorCodes.DUPLICATE_ENTITY
                response.body?.message shouldBe "User with email already exists"
            }
        }
        
        context("기타 PersistenceException 처리") {
            it("기타 PersistenceException을 500 Internal Server Error로 변환해야 한다") {
                // given
                val exception = object : PersistenceException("Database connection failed") {}
                
                // when
                val response = handler.handlePersistenceException(exception, request)
                
                // then
                response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
                response.body?.code shouldBe ErrorCodes.DATABASE_ERROR
                response.body?.message shouldBe "Database connection failed"
            }
        }
    }
    
    describe("handleValidationException") {
        
        it("MethodArgumentNotValidException을 400 Bad Request로 변환해야 한다") {
            // given
            val target = object {
                var name: String = ""
                var email: String = ""
            }
            val bindingResult = BeanPropertyBindingResult(target, "user")
            bindingResult.addError(FieldError("user", "name", "Name is required"))
            bindingResult.addError(FieldError("user", "email", "Email format is invalid"))
            
            val parameter = mockk<org.springframework.core.MethodParameter>(relaxed = true)
            val exception = MethodArgumentNotValidException(parameter, bindingResult)
            
            // when
            val response = handler.handleValidationException(exception, request)
            
            // then
            response.statusCode shouldBe HttpStatus.BAD_REQUEST
            response.body?.code shouldBe ErrorCodes.VALIDATION_FAILED
            response.body?.message shouldBe "Request validation failed"
            response.body?.details?.get("name") shouldBe "Name is required"
            response.body?.details?.get("email") shouldBe "Email format is invalid"
        }
    }
    
    describe("handleGenericException") {
        
        it("예상하지 못한 예외를 500 Internal Server Error로 변환해야 한다") {
            // given
            val exception = RuntimeException("Unexpected error occurred")
            
            // when
            val response = handler.handleGenericException(exception, request)
            
            // then
            response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
            response.body?.code shouldBe ErrorCodes.INTERNAL_ERROR
            response.body?.message shouldBe "Internal server error"
        }
    }
    
    describe("디버그 정보 생성") {
        
        context("개발 환경에서") {
            it("디버그 정보가 포함되어야 한다") {
                // given
                every { environment.activeProfiles } returns arrayOf("dev")
                val exception = DomainValidationException("Test exception")
                
                // when
                val response = handler.handleDomainException(exception, request)
                
                // then
                response.body?.debug shouldNotBe null
                response.body?.debug?.path shouldBe "/api/v1/users"
                response.body?.debug?.method shouldBe "POST"
                response.body?.debug?.exceptionType shouldBe "DomainValidationException"
            }
        }
        
        context("운영 환경에서") {
            it("디버그 정보가 포함되지 않아야 한다") {
                // given
                every { environment.activeProfiles } returns arrayOf("prod")
                val exception = DomainValidationException("Test exception")
                
                // when
                val response = handler.handleDomainException(exception, request)
                
                // then
                response.body?.debug shouldBe null
            }
        }
    }
    
    describe("추적 ID 생성") {
        
        it("MDC에서 추적 ID를 가져와야 한다") {
            // given
            val exception = DomainValidationException("Test exception")
            
            // when
            val response = handler.handleDomainException(exception, request)
            
            // then
            response.body?.traceId shouldNotBe null
            response.body?.traceId?.length shouldBe 36 // UUID format
        }
    }
})
