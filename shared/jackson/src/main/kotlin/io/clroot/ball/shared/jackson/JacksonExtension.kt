package io.clroot.ball.shared.jackson

import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.jsonMapper

val mapper =
    jsonMapper {
        addModule(
            KotlinModule
                .Builder()
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, true)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
        ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

fun <T : Any> T.toJsonString(): String = mapper.writeValueAsString(this)

@Suppress("UNCHECKED_CAST")
fun <T : Any> T.toMap(): Map<String, Any?> = mapper.convertValue(this, Map::class.java) as Map<String, Any?>
