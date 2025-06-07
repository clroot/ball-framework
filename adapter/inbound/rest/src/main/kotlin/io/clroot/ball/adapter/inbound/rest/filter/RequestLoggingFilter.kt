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
        filterChain: FilterChain
    ) {
        val traceId = getOrCreateTraceId(request)
        MDC.put(TRACE_ID_MDC_KEY, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)
        
        val startTime = System.currentTimeMillis()
        
        val cachedRequest = ContentCachingRequestWrapper(request)
        val cachedResponse = ContentCachingResponseWrapper(response)
        
        try {
            logRequest(cachedRequest)
            filterChain.doFilter(cachedRequest, cachedResponse)
        } finally {
            val executionTime = System.currentTimeMillis() - startTime
            logResponse(cachedResponse, executionTime)
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
    
    private fun logRequest(request: ContentCachingRequestWrapper) {
        val uri = request.requestURI
        val queryString = request.queryString
        val method = request.method
        val contentType = request.contentType ?: ""
        val userAgent = request.getHeader("User-Agent") ?: ""
        val fullUrl = if (queryString.isNullOrBlank()) uri else "$uri?$queryString"
        
        log.info("Request: {} {} (Content-Type: {}, User-Agent: {})", method, fullUrl, contentType, userAgent)
        
        if (shouldLogRequestBody(contentType)) {
            val content = getRequestContent(request)
            if (content.isNotBlank()) {
                log.debug("Request body: {}", content)
            }
        }
    }
    
    private fun logResponse(response: ContentCachingResponseWrapper, executionTime: Long) {
        val status = response.status
        val contentType = response.contentType ?: ""
        
        log.info("Response: {} ({}ms, Content-Type: {})", status, executionTime, contentType)
        
        if (shouldLogResponseBody(contentType)) {
            val content = getResponseContent(response)
            if (content.isNotBlank()) {
                log.debug("Response body: {}", content)
            }
        }
    }
    
    private fun shouldLogRequestBody(contentType: String): Boolean {
        return contentType.contains("application/json") || 
               contentType.contains("application/xml") ||
               contentType.contains("text/plain") ||
               contentType.contains("text/html")
    }
    
    private fun shouldLogResponseBody(contentType: String): Boolean {
        return contentType.contains("application/json") || 
               contentType.contains("application/xml") ||
               contentType.contains("text/plain") ||
               contentType.contains("text/html")
    }
    
    private fun getRequestContent(request: ContentCachingRequestWrapper): String {
        val content = request.contentAsByteArray
        if (content.isEmpty()) return ""
        
        return try {
            val contentString = String(content, Charsets.UTF_8)
            if (contentString.length > MAX_PAYLOAD_LENGTH) {
                contentString.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)"
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
                contentString.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)"
            } else {
                contentString
            }
        } catch (e: UnsupportedEncodingException) {
            "Error reading response body: ${e.message}"
        }
    }
}