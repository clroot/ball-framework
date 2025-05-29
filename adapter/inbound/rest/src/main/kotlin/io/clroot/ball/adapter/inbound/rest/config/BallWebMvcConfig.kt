package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.converter.StringToPageRequestConverter
import io.clroot.ball.adapter.inbound.rest.converter.StringToSortConverter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Import(
    StringToPageRequestConverter::class,
    StringToSortConverter::class
)
class BallWebMvcConfig(
    private val pageRequestConverter: StringToPageRequestConverter,
    private val sortConverter: StringToSortConverter
) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(pageRequestConverter)
        registry.addConverter(sortConverter)
    }
}