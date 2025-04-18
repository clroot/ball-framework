package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.versioning.ApiVersionRequestMappingHandlerMapping
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * Spring Web MVC 설정
 * 
 * 이 클래스는 Spring Web MVC 관련 설정을 제공합니다.
 */
@Configuration
open class WebMvcConfig {

    /**
     * API 버전 지원을 위한 RequestMappingHandlerMapping 빈 등록
     */
    @Bean
    open fun requestMappingHandlerMapping(): RequestMappingHandlerMapping {
        val handlerMapping = ApiVersionRequestMappingHandlerMapping()
        handlerMapping.order = 0
        return handlerMapping
    }
}
