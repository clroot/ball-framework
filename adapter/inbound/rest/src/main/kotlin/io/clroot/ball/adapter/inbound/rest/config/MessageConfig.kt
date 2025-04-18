package io.clroot.ball.adapter.inbound.rest.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import java.util.Locale

/**
 * 메시지 및 국제화 설정
 * 
 * 이 클래스는 다국어 메시지 및 오류 응답을 지원하기 위한 설정을 제공합니다.
 */
@Configuration
open class MessageConfig : WebMvcConfigurer {

    /**
     * 로케일 리졸버 설정
     * 
     * Accept-Language 헤더를 기반으로 로케일을 결정합니다.
     */
    @Bean
    open fun localeResolver(): LocaleResolver {
        return object : AcceptHeaderLocaleResolver() {
            private val supportedLocales = listOf(
                Locale.KOREAN,
                Locale.ENGLISH,
                Locale.JAPANESE,
                Locale.CHINESE
            )

            override fun resolveLocale(request: jakarta.servlet.http.HttpServletRequest): Locale {
                val acceptLanguage = request.getHeader("Accept-Language")
                if (acceptLanguage.isNullOrEmpty()) {
                    return Locale.KOREAN // 기본 로케일은 한국어
                }

                // Accept-Language 헤더에서 로케일 추출
                val locale = Locale.LanguageRange.parse(acceptLanguage)
                    .asSequence()
                    .map { range -> Locale.forLanguageTag(range.range) }
                    .firstOrNull { locale -> supportedLocales.contains(locale) }

                return locale ?: Locale.KOREAN
            }
        }
    }

    /**
     * 로케일 변경 인터셉터 설정
     * 
     * 'lang' 파라미터를 통해 로케일을 변경할 수 있습니다.
     */
    @Bean
    open fun localeChangeInterceptor(): LocaleChangeInterceptor {
        val interceptor = LocaleChangeInterceptor()
        interceptor.paramName = "lang"
        return interceptor
    }

    /**
     * 인터셉터 등록
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }

    /**
     * 메시지 소스 설정
     */
    @Bean
    open fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasenames(
            "classpath:messages/messages",
            "classpath:messages/validation"
        )
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setCacheSeconds(60) // 개발 환경에서는 캐시 시간을 짧게 설정
        return messageSource
    }

    /**
     * 검증 메시지 소스 설정
     */
    @Bean
    open fun validator(): LocalValidatorFactoryBean {
        val bean = LocalValidatorFactoryBean()
        bean.setValidationMessageSource(messageSource())
        return bean
    }
}
