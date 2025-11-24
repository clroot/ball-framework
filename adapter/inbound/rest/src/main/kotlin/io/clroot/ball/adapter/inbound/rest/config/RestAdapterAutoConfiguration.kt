package io.clroot.ball.adapter.inbound.rest.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.context.annotation.ComponentScan
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
@ComponentScan(basePackages = ["io.clroot.ball.adapter.inbound.rest"])
class RestAdapterAutoConfiguration
