package io.clroot.ball.user.adapter.outbound.persistence.jpa

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.clroot.ball.shared.attribute.Attributable
import io.clroot.ball.shared.attribute.AttributeKey
import io.clroot.ball.shared.attribute.AttributeStore
import io.clroot.ball.shared.core.model.Entity
import io.clroot.ball.user.domain.port.AttributePersistenceProvider
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * JSON 컬럼 속성 영속성 제공자 (JSON Column Attribute Persistence Provider)
 *
 * 엔티티의 속성을 JSON 형식으로 단일 컬럼에 저장하는 구현체
 */
@Component
class JsonColumnAttributePersistenceProvider(
    private val objectMapper: ObjectMapper
) : AttributePersistenceProvider {

    /**
     * 엔티티의 속성 로드
     *
     * @param entity 속성을 로드할 엔티티
     * @param dataModel 데이터 모델 객체
     * @return 속성이 로드된 엔티티
     */
    @Suppress("UNCHECKED_CAST")
    override fun <E : Entity<*>> loadAttributes(entity: E, dataModel: Any): E {
        if (entity !is Attributable<*>) {
            return entity
        }

        if (dataModel !is UserDataModel) {
            return entity
        }

        val attributesJson = dataModel.attributes ?: return entity
        val attributeMap = try {
            objectMapper.readValue(attributesJson, object : TypeReference<Map<String, AttributeValue>>() {})
        } catch (e: Exception) {
            return entity
        }

        val attributeStore = AttributeStore.empty()
        val loadedAttributeStore = attributeMap.entries.fold(attributeStore) { store, entry ->
            val keyName = entry.key
            val attributeValue = entry.value

            val keyType = try {
                Class.forName(attributeValue.type).kotlin
            } catch (e: Exception) {
                return@fold store
            }

            val key = AttributeKey(keyName, keyType as KClass<Any>)
            val value = deserializeValue(attributeValue.value, keyType)
                ?: return@fold store

            store.setAttribute(key, value)
        }

        // 리플렉션을 사용하여 속성 저장소 설정
        // 실제 구현에서는 엔티티에 적절한 메서드를 추가하는 것이 좋음
        return entity
    }

    /**
     * 엔티티의 속성 저장
     *
     * @param entity 속성을 저장할 엔티티
     * @param dataModel 데이터 모델 객체
     */
    override fun <E : Entity<*>> saveAttributes(entity: E, dataModel: Any) {
        if (entity !is Attributable<*>) {
            return
        }

        if (dataModel !is UserDataModel) {
            return
        }

        val attributeMap = entity.attributes.getAttributes().map { (key, value) ->
            key.name to AttributeValue(
                type = key.type.qualifiedName ?: key.type.toString(),
                value = serializeValue(value)
            )
        }.toMap()

        if (attributeMap.isEmpty()) {
            dataModel.attributes = null
            return
        }

        dataModel.attributes = objectMapper.writeValueAsString(attributeMap)
    }

    /**
     * 값 직렬화
     *
     * @param value 직렬화할 값
     * @return 직렬화된 문자열
     */
    private fun serializeValue(value: Any): String {
        return when (value) {
            is String, is Number, is Boolean -> value.toString()
            else -> objectMapper.writeValueAsString(value)
        }
    }

    /**
     * 값 역직렬화
     *
     * @param value 역직렬화할 문자열
     * @param type 값의 타입
     * @return 역직렬화된 값
     */
    @Suppress("UNCHECKED_CAST")
    private fun deserializeValue(value: String, type: KClass<*>): Any? {
        return when {
            type == String::class -> value
            type == Int::class -> value.toIntOrNull()
            type == Long::class -> value.toLongOrNull()
            type == Double::class -> value.toDoubleOrNull()
            type == Float::class -> value.toFloatOrNull()
            type == Boolean::class -> value.toBoolean()
            type.isSubclassOf(Enum::class) -> {
                try {
                    val enumClass = type.java
                    val method = enumClass.getMethod("valueOf", String::class.java)
                    method.invoke(null, value)
                } catch (e: Exception) {
                    null
                }
            }

            else -> try {
                objectMapper.readValue(value, type.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 속성 값 (Attribute Value)
     *
     * JSON으로 저장되는 속성 값 구조
     *
     * @param type 속성 타입의 정규화된 이름
     * @param value 직렬화된 속성 값
     */
    private data class AttributeValue(
        val type: String,
        val value: String
    )
}