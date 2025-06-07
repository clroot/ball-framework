package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.converter.StringToPageRequestConverter
import io.clroot.ball.adapter.inbound.rest.converter.StringToSortConverter
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.mockk
import io.mockk.verify
import org.springframework.format.FormatterRegistry

/**
 * 기본 Web Config 테스트
 */
class RestAdapterWebConfigTest : DescribeSpec({

    describe("RestAdapterWebConfig") {
        
        it("컨버터들을 등록한다") {
            // given
            val pageRequestConverter = mockk<StringToPageRequestConverter>(relaxed = true)
            val sortConverter = mockk<StringToSortConverter>(relaxed = true)
            val registry = mockk<FormatterRegistry>(relaxed = true)
            
            val config = RestAdapterWebConfig(pageRequestConverter, sortConverter)
            
            // when
            config.addFormatters(registry)
            
            // then
            verify { registry.addConverter(pageRequestConverter) }
            verify { registry.addConverter(sortConverter) }
        }
    }
})
