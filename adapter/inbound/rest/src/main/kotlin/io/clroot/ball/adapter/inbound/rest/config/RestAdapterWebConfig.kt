package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.converter.StringToPageRequestConverter
import io.clroot.ball.adapter.inbound.rest.converter.StringToSortConverter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * REST Adapter Web 설정
 * 
 * Inbound REST Adapter에서 사용할 Web MVC 설정을 담당합니다.
 * - 커스텀 컨버터 등록
 * - 포맷터 설정
 */
@Configuration
@Import(
    StringToPageRequestConverter::class,
    StringToSortConverter::class
)
class RestAdapterWebConfig(
    private val pageRequestConverter: StringToPageRequestConverter,
    private val sortConverter: StringToSortConverter
) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(pageRequestConverter)
        registry.addConverter(sortConverter)
    }
}