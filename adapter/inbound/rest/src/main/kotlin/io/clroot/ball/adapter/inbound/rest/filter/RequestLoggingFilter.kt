package io.clroot.ball.adapter.inbound.rest.filter

import io.clroot.ball.domain.slf4j
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.UnsupportedEncodingException
import java.util.*

/**
 * 요청 로깅 필터
 *
 * 이 필터는 모든 HTTP 요청과 응답을 로깅합니다.
 * 또한 각 요청에 고유한 추적 ID를 할당하여 로그에 포함시킵니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = slf4j()

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_MDC_KEY = "traceId"
        private const val MAX_PAYLOAD_LENGTH = 10000
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = getOrCreateTraceId(request)
        MDC.put(TRACE_ID_MDC_KEY, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        val startTime = System.currentTimeMillis()

        val cachedRequest = ContentCachingRequestWrapper(request, 0)
        val cachedResponse = ContentCachingResponseWrapper(response)

        try {
            filterChain.doFilter(cachedRequest, cachedResponse)
        } finally {
            val executionTime = System.currentTimeMillis() - startTime
            logRequestAndResponse(cachedRequest, cachedResponse, executionTime)
            cachedResponse.copyBodyToResponse()
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }

    private fun getOrCreateTraceId(request: HttpServletRequest): String {
        val traceId = request.getHeader(TRACE_ID_HEADER)
        return if (traceId.isNullOrBlank()) {
            UUID.randomUUID().toString()
        } else {
            traceId
        }
    }

    private fun logRequestAndResponse(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        executionTime: Long,
    ) {
        val uri = request.requestURI
        val queryString = request.queryString
        val method = request.method
        val fullUrl = if (queryString.isNullOrBlank()) uri else "$uri?$queryString"
        val status = response.status

        // 기본 정보 로깅 (한 줄로)
        log.info(
            "HTTP {} {} -> {} ({}ms)",
            method,
            fullUrl,
            status,
            executionTime,
        )

        // 상세 정보는 debug 레벨로
        if (log.isDebugEnabled) {
            val requestContentType = request.contentType ?: ""
            val responseContentType = response.contentType ?: ""
            val userAgent = request.getHeader("User-Agent") ?: ""

            val logDetails =
                buildString {
                    appendLine("HTTP Transaction Details:")
                    appendLine("  Request:")
                    appendLine("    Method: $method")
                    appendLine("    URL: $fullUrl")
                    appendLine("    Content-Type: $requestContentType")
                    appendLine("    User-Agent: $userAgent")

                    if (shouldLogRequestBody(requestContentType)) {
                        val requestContent = getRequestContent(request)
                        if (requestContent.isNotBlank()) {
                            appendLine("    Body: $requestContent")
                        }
                    }

                    appendLine("  Response:")
                    appendLine("    Status: $status")
                    appendLine("    Content-Type: $responseContentType")
                    appendLine("    Execution Time: ${executionTime}ms")

                    if (shouldLogResponseBody(responseContentType)) {
                        val responseContent = getResponseContent(response)
                        if (responseContent.isNotBlank()) {
                            appendLine("    Body: $responseContent")
                        }
                    }
                }

            log.debug(logDetails)
        }
    }

    private fun shouldLogRequestBody(contentType: String): Boolean =
        contentType.contains("application/json") ||
            contentType.contains("application/xml") ||
            contentType.contains("text/plain") ||
            contentType.contains("text/html")

    private fun shouldLogResponseBody(contentType: String): Boolean =
        contentType.contains("application/json") ||
            contentType.contains("application/xml") ||
            contentType.contains("text/plain") ||
            contentType.contains("text/html")

    private fun getRequestContent(request: ContentCachingRequestWrapper): String {
        val content = request.contentAsByteArray
        if (content.isEmpty()) return ""

        return try {
            val contentString = String(content, Charsets.UTF_8)
            if (contentString.length > MAX_PAYLOAD_LENGTH) {
                contentString.take(MAX_PAYLOAD_LENGTH) + "... (truncated)"
            } else {
                contentString
            }
        } catch (e: UnsupportedEncodingException) {
            "Error reading request body: ${e.message}"
        }
    }

    private fun getResponseContent(response: ContentCachingResponseWrapper): String {
        val content = response.contentAsByteArray
        if (content.isEmpty()) return ""

        return try {
            val contentString = String(content, Charsets.UTF_8)
            if (contentString.length > MAX_PAYLOAD_LENGTH) {
                contentString.take(MAX_PAYLOAD_LENGTH) + "... (truncated)"
            } else {
                contentString
            }
        } catch (e: UnsupportedEncodingException) {
            "Error reading response body: ${e.message}"
        }
    }
}
