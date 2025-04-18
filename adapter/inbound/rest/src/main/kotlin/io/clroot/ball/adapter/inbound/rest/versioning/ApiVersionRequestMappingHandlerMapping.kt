package io.clroot.ball.adapter.inbound.rest.versioning

import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.reflect.Method

/**
 * API 버전 요청 매핑 핸들러
 * 
 * 이 클래스는 ApiVersion 어노테이션을 처리하여 URL 경로에 버전 정보를 포함시킵니다.
 */
class ApiVersionRequestMappingHandlerMapping : RequestMappingHandlerMapping() {

    override fun getMappingForMethod(method: Method, handlerType: Class<*>): RequestMappingInfo? {
        var info = super.getMappingForMethod(method, handlerType)
        if (info == null) return null

        // 메서드에서 ApiVersion 어노테이션 찾기
        val methodAnnotation = AnnotationUtils.findAnnotation(method, ApiVersion::class.java)
        if (methodAnnotation != null) {
            info = createApiVersionInfo(methodAnnotation, info)
            return info
        }

        // 클래스에서 ApiVersion 어노테이션 찾기
        val classAnnotation = AnnotationUtils.findAnnotation(handlerType, ApiVersion::class.java)
        if (classAnnotation != null) {
            info = createApiVersionInfo(classAnnotation, info)
            return info
        }

        return info
    }

    private fun createApiVersionInfo(annotation: ApiVersion, info: RequestMappingInfo): RequestMappingInfo {
        val version = annotation.value

        // URL 패턴에서 {version} 플레이스홀더를 실제 버전으로 대체
        val patterns = info.patternsCondition!!.patterns
            .map { pattern -> pattern.replace("{version}", version) }
            .toSet()

        val newPatternsCondition = PatternsRequestCondition(*patterns.toTypedArray())

        return RequestMappingInfo(
            newPatternsCondition,
            info.methodsCondition,
            info.paramsCondition,
            info.headersCondition,
            info.consumesCondition,
            info.producesCondition,
            info.customCondition
        )
    }
}