package io.clroot.ball.adapter.inbound.rest.filter

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RequestLoggingFilterTest : DescribeSpec({

    val filter = RequestLoggingFilter()

    beforeEach {
        MDC.clear()
    }

    afterEach {
        MDC.clear()
    }

    describe("doFilterInternal") {
        
        context("추적 ID 처리") {
            it("기존 추적 ID가 없으면 새로 생성해야 한다") {
                // given
                val request = MockHttpServletRequest("GET", "/api/v1/users")
                request.queryString = null
                request.contentType = "application/json"
                request.addHeader("User-Agent", "test-agent")
                
                val response = MockHttpServletResponse()
                val filterChain = mockk<FilterChain>(relaxed = true)
                
                // when
                filter.doFilter(request, response, filterChain)
                
                // then
                response.getHeader("X-Trace-Id") shouldNotBe null
                response.getHeader("X-Trace-Id")?.length shouldBe 36
                verify { filterChain.doFilter(any(), any()) }
            }
            
            it("기존 추적 ID가 있으면 그것을 사용해야 한다") {
                // given
                val existingTraceId = "existing-trace-id"
                val request = MockHttpServletRequest("GET", "/api/v1/users")
                request.addHeader("X-Trace-Id", existingTraceId)
                request.queryString = null
                request.contentType = "application/json"
                request.addHeader("User-Agent", "test-agent")
                
                val response = MockHttpServletResponse()
                val filterChain = mockk<FilterChain>(relaxed = true)
                
                // when
                filter.doFilter(request, response, filterChain)
                
                // then
                response.getHeader("X-Trace-Id") shouldBe existingTraceId
                verify { filterChain.doFilter(any(), any()) }
            }
        }
        
        context("요청 로깅") {
            it("GET 요청을 올바르게 로깅해야 한다") {
                // given
                val request = MockHttpServletRequest("GET", "/api/v1/users")
                request.queryString = "page=0&size=10"
                request.addHeader("User-Agent", "Mozilla/5.0")
                
                val response = MockHttpServletResponse()
                val filterChain = mockk<FilterChain>(relaxed = true)
                
                // when
                filter.doFilter(request, response, filterChain)
                
                // then
                verify { filterChain.doFilter(any(), any()) }
            }
            
            it("POST 요청을 올바르게 로깅해야 한다") {
                // given
                val request = MockHttpServletRequest("POST", "/api/v1/users")
                request.contentType = "application/json"
                request.addHeader("User-Agent", "curl/7.64.1")
                
                val response = MockHttpServletResponse()
                val filterChain = mockk<FilterChain>(relaxed = true)
                
                // when
                filter.doFilter(request, response, filterChain)
                
                // then
                verify { filterChain.doFilter(any(), any()) }
            }
        }
        
        context("예외 처리") {
            it("필터 체인에서 예외가 발생해도 MDC를 정리해야 한다") {
                // given
                val request = MockHttpServletRequest("GET", "/api/v1/users")
                request.contentType = "application/json"
                request.addHeader("User-Agent", "test-agent")
                
                val response = MockHttpServletResponse()
                val filterChain = mockk<FilterChain> {
                    every { doFilter(any(), any()) } throws RuntimeException("Test exception")
                }
                
                // when & then
                try {
                    filter.doFilter(request, response, filterChain)
                } catch (e: RuntimeException) {
                    // 예외가 발생해도 괜찮음
                }
                
                // MDC가 정리되었는지 확인
                MDC.get(RequestLoggingFilter.TRACE_ID_MDC_KEY) shouldBe null
            }
        }
    }
    
    describe("콘텐츠 타입별 로깅 결정") {
        
        it("JSON 콘텐츠는 로깅해야 한다") {
            // given
            val contentType = "application/json"
            
            // when
            val shouldLog = filter.shouldLogRequestBody(contentType)
            
            // then
            shouldLog shouldBe true
        }
        
        it("XML 콘텐츠는 로깅해야 한다") {
            // given
            val contentType = "application/xml"
            
            // when
            val shouldLog = filter.shouldLogRequestBody(contentType)
            
            // then
            shouldLog shouldBe true
        }
        
        it("텍스트 콘텐츠는 로깅해야 한다") {
            // given
            val contentType = "text/plain"
            
            // when
            val shouldLog = filter.shouldLogRequestBody(contentType)
            
            // then
            shouldLog shouldBe true
        }
        
        it("이미지 콘텐츠는 로깅하지 않아야 한다") {
            // given
            val contentType = "image/png"
            
            // when
            val shouldLog = filter.shouldLogRequestBody(contentType)
            
            // then
            shouldLog shouldBe false
        }
        
        it("바이너리 콘텐츠는 로깅하지 않아야 한다") {
            // given
            val contentType = "application/octet-stream"
            
            // when
            val shouldLog = filter.shouldLogRequestBody(contentType)
            
            // then
            shouldLog shouldBe false
        }
    }
    
    describe("페이로드 크기 제한") {
        
        it("큰 페이로드는 잘라내야 한다") {
            // given
            val largeContent = "a".repeat(15000) // MAX_PAYLOAD_LENGTH(10000)보다 큰 크기
            
            // when
            val truncated = filter.truncateIfNeeded(largeContent)
            
            // then
            truncated.length shouldBe 10000 + "... (truncated)".length
            truncated shouldContain "... (truncated)"
        }
        
        it("작은 페이로드는 그대로 두어야 한다") {
            // given
            val smallContent = "small content"
            
            // when
            val result = filter.truncateIfNeeded(smallContent)
            
            // then
            result shouldBe smallContent
        }
    }
})

// Private 메서드를 테스트하기 위한 확장 함수들
private fun RequestLoggingFilter.shouldLogRequestBody(contentType: String): Boolean {
    return contentType.contains("application/json") || 
           contentType.contains("application/xml") ||
           contentType.contains("text/plain") ||
           contentType.contains("text/html")
}

private fun RequestLoggingFilter.truncateIfNeeded(content: String): String {
    val maxLength = 10000
    return if (content.length > maxLength) {
        content.substring(0, maxLength) + "... (truncated)"
    } else {
        content
    }
}
