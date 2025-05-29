package io.clroot.ball.shared.lock

import io.clroot.ball.shared.lock.exception.LockKeyResolutionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.concurrent.ConcurrentHashMap

/**
 * @LockKey 어노테이션 기반 분산 락 키 해석기
 */
interface DistributedLockAnnotationProcessor {
    /**
     * 메서드에 적용된 분산 락 애노테이션 처리
     *
     * @param annotation 락 애노테이션
     * @param method 대상 메서드
     * @param args 메서드 인자 배열
     * @param parameterNames 메서드 파라미터 이름 배열
     * @return 실제 락 키
     */
    fun resolveKey(annotation: DistributedLock, method: Method, args: Array<Any?>, parameterNames: Array<String>): String
}

@Component
class LockKeyAnnotationProcessor : DistributedLockAnnotationProcessor {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    // 메서드별 @LockKey 메타데이터 캐싱 (성능 최적화)
    private val methodCache = ConcurrentHashMap<Method, List<LockKeyMetadata>>()
    
    override fun resolveKey(annotation: DistributedLock, method: Method, args: Array<Any?>, parameterNames: Array<String>): String {
        val lockKeyMetadata = methodCache.computeIfAbsent(method) { extractLockKeyMetadata(it) }
        
        val keyValues = lockKeyMetadata.associate { metadata ->
            val value = extractValue(args[metadata.paramIndex], metadata)
            metadata.keyName to value
        }
        
        log.debug("Resolved lock key values: {} for template: {}", keyValues, annotation.key)
        
        return replacePlaceholders(annotation.key, keyValues)
    }
    
    private fun extractLockKeyMetadata(method: Method): List<LockKeyMetadata> {
        return method.parameters.mapIndexedNotNull { index, parameter ->
            val lockKeyAnnotation = parameter.getAnnotation(LockKey::class.java)
            if (lockKeyAnnotation != null) {
                val keyName = lockKeyAnnotation.value.ifEmpty { parameter.name }
                LockKeyMetadata(
                    paramIndex = index,
                    keyName = keyName,
                    property = lockKeyAnnotation.property.takeIf { it.isNotEmpty() },
                    nullValue = lockKeyAnnotation.nullValue,
                    parameter = parameter
                )
            } else null
        }
    }
    
    private fun extractValue(arg: Any?, metadata: LockKeyMetadata): String {
        if (arg == null) return metadata.nullValue
        
        return if (metadata.property != null) {
            extractProperty(arg, metadata.property, metadata.nullValue)
        } else {
            arg.toString()
        }
    }
    
    private fun extractProperty(obj: Any, propertyPath: String, nullValue: String): String {
        return try {
            var current: Any? = obj
            
            // 중첩 프로퍼티 지원 (예: "user.profile.id")
            for (propertyName in propertyPath.split(".")) {
                if (current == null) return nullValue
                current = getPropertyValue(current, propertyName)
            }
            
            current?.toString() ?: nullValue
        } catch (e: Exception) {
            log.warn("Failed to extract property '{}' from {}: {}", propertyPath, obj.javaClass.simpleName, e.message)
            nullValue  // Graceful fallback for production stability
        }
    }
    
    private fun getPropertyValue(obj: Any, propertyName: String): Any? {
        val clazz = obj.javaClass
        
        // 1. 필드 직접 접근 시도
        try {
            val field = findField(clazz, propertyName)
            if (field != null) {
                field.isAccessible = true
                return field.get(obj)
            }
        } catch (e: Exception) {
            // 필드 접근 실패시 계속 진행
        }
        
        // 2. Getter 메서드 시도
        try {
            val getter = findGetter(clazz, propertyName)
            if (getter != null) {
                return getter.invoke(obj)
            }
        } catch (e: Exception) {
            // Getter 접근 실패시 계속 진행
        }
        
        throw LockKeyResolutionException("Cannot access property '$propertyName' in ${clazz.simpleName}")
    }
    
    private fun findField(clazz: Class<*>, fieldName: String): java.lang.reflect.Field? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }
    
    private fun findGetter(clazz: Class<*>, propertyName: String): Method? {
        val getterNames = listOf(
            "get${propertyName.replaceFirstChar { it.uppercase() }}",
            "is${propertyName.replaceFirstChar { it.uppercase() }}",
            propertyName  // Kotlin property style
        )
        
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            for (getterName in getterNames) {
                try {
                    return currentClass.getDeclaredMethod(getterName)
                } catch (e: NoSuchMethodException) {
                    // 다음 getter 이름 시도
                }
            }
            currentClass = currentClass.superclass
        }
        
        return null
    }
    
    private fun replacePlaceholders(template: String, keyValues: Map<String, String>): String {
        var result = template
        
        keyValues.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        
        // 치환되지 않은 플레이스홀더 검증
        val unresolved = Regex("\\{([^}]+)\\}").findAll(result).map { it.groupValues[1] }.toList()
        if (unresolved.isNotEmpty()) {
            throw LockKeyResolutionException(
                "Unresolved placeholders in lock key template '$template': $unresolved. " +
                "Make sure all placeholders have corresponding @LockKey annotations."
            )
        }
        
        return result
    }
    
    private data class LockKeyMetadata(
        val paramIndex: Int,
        val keyName: String,
        val property: String?,
        val nullValue: String,
        val parameter: Parameter
    )
}
