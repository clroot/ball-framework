package io.clroot.ball.adapter.inbound.rest.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 응답 압축 설정
 * 
 * 이 클래스는 HTTP 응답 압축을 설정합니다.
 * 
 * 압축 설정은 application.properties 또는 application.yml 파일에서 다음과 같이 설정할 수 있습니다:
 * 
 * ```
 * server.compression.enabled=true
 * server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain
 * server.compression.min-response-size=2048
 * ```
 */
@Configuration
@PropertySource("classpath:compression.properties")
open class CompressionConfig : WebMvcConfigurer {
    // 압축 설정은 application.properties 또는 compression.properties 파일에서 설정됩니다.
}
