package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.converter.StringToPageRequestConverter
import io.clroot.ball.adapter.inbound.rest.converter.StringToSortConverter
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * REST Adapter Web 설정
 * 기존 구현된 컨버터들을 등록합니다.
 */
@Configuration
class RestAdapterWebConfig(
    private val pageRequestConverter: StringToPageRequestConverter,
    private val sortConverter: StringToSortConverter
) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(pageRequestConverter)
        registry.addConverter(sortConverter)
    }
}
