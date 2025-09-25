package io.clroot.ball.adapter.inbound.rest.config

import org.springframework.core.annotation.Order
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Order(10)
class WebConfiguration : WebMvcConfigurer {
    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        val resolver = PageableHandlerMethodArgumentResolver()
        resolver.setFallbackPageable(PageRequest.of(0, 30, Sort.by("id").descending()))
        argumentResolvers.add(resolver)
    }
}
