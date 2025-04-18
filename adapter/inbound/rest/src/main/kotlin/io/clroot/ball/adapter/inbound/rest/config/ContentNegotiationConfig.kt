package io.clroot.ball.adapter.inbound.rest.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.StringHttpMessageConverter
import java.nio.charset.StandardCharsets

/**
 * 컨텐츠 협상 설정
 * 
 * 이 클래스는 다양한 응답 형식(JSON, CSV 등)을 지원하기 위한 설정을 제공합니다.
 */
@Configuration
open class ContentNegotiationConfig : WebMvcConfigurer {

    /**
     * 컨텐츠 협상 설정
     */
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer
            .favorParameter(true)
            .parameterName("format")
            .ignoreAcceptHeader(false)
            .useRegisteredExtensionsOnly(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("csv", MediaType.parseMediaType("text/csv"))
            .mediaType("text", MediaType.TEXT_PLAIN)
    }

    /**
     * HTTP 메시지 컨버터 설정
     */
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // 문자열 컨버터 (UTF-8 인코딩)
        val stringConverter = StringHttpMessageConverter(StandardCharsets.UTF_8)
        converters.add(stringConverter)

        // JSON 컨버터
        val jsonConverter = MappingJackson2HttpMessageConverter(objectMapper())
        converters.add(jsonConverter)

        // CSV 컨버터는 필요에 따라 커스텀 구현 가능
    }

    /**
     * ObjectMapper 빈 설정
     */
    @Bean
    open fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }
}
