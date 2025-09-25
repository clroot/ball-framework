package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.exception.GlobalExceptionHandler
import io.clroot.ball.adapter.inbound.rest.filter.RequestLoggingFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.web.servlet.DispatcherServlet

/**
 * Ball Framework REST Adapter Auto Configuration
 * 기존 구현된 클래스들을 자동으로 빈으로 등록합니다.
 */
@AutoConfiguration(after = [JacksonAutoConfiguration::class, WebMvcAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet::class)
@ConditionalOnProperty(
    prefix = "ball.adapter.rest",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class RestAdapterAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun globalExceptionHandler(environment: Environment): GlobalExceptionHandler = GlobalExceptionHandler(environment)

    @Bean
    @ConditionalOnMissingBean
    fun requestLoggingFilter(): RequestLoggingFilter = RequestLoggingFilter()

    @Bean
    fun ballJacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer = BallJackson2ObjectMapperBuilderCustomizer()

    @Bean
    fun webConfiguration(): WebConfiguration = WebConfiguration()
}
