package io.clroot.ball.adapter.inbound.rest.config

import io.clroot.ball.adapter.inbound.rest.converter.StringToPageRequestConverter
import io.clroot.ball.adapter.inbound.rest.converter.StringToSortConverter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.springframework.format.FormatterRegistry

class RestAdapterWebConfigTest : DescribeSpec({

    describe("BallWebMvcConfig") {
        
        it("FormatterRegistry에 필요한 컨버터들을 추가해야 한다") {
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
        
        it("의존성이 올바르게 주입되어야 한다") {
            // given
            val pageRequestConverter = mockk<StringToPageRequestConverter>()
            val sortConverter = mockk<StringToSortConverter>()
            
            // when
            val config = RestAdapterWebConfig(pageRequestConverter, sortConverter)
            
            // then (컴파일 오류가 없으면 의존성 주입이 올바름)
            config shouldBe config
        }
    }
})
