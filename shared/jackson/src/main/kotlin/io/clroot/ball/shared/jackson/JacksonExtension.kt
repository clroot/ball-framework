package io.clroot.ball.shared.jackson

import arrow.integrations.jackson.module.registerArrowModule
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

private val mapper =
    jacksonObjectMapper().apply {
        registerBallModule()
    }

fun ObjectMapper.registerBallModule() {
    // Java Time 모듈 등록 (JSR-310)
    registerModule(JavaTimeModule())

    // Kotlin 모듈 등록
    registerKotlinModule()

    // Arrow 함수형 프로그래밍 모듈 등록
    registerArrowModule()

    // 타임스탬프 대신 ISO 형식 사용
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    // 알 수 없는 프로퍼티에 대해 실패하지 않음
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    // null 값은 JSON에 포함하지 않음
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
}

fun <T : Any> T.toJsonString(): String = mapper.writeValueAsString(this)

@Suppress("UNCHECKED_CAST")
fun <T : Any> T.toMap(): Map<String, Any?> = mapper.convertValue(this, Map::class.java) as Map<String, Any?>
