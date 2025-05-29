package io.clroot.ball.shared.lock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/**
 * @DistributedLock 어노테이션이 적용된 메서드에 AOP를 적용하여 분산 락을 처리
 */
@Aspect
@Component
@Order(HIGHEST_PRECEDENCE)
class DistributedLockAdvisor(
    private val lockProvider: LockProvider,
    private val annotationProcessor: DistributedLockAnnotationProcessor
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(io.clroot.ball.shared.lock.DistributedLock)")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(DistributedLock::class.java)

        requireNotNull(annotation) { "Failed to resolve DistributedLock annotation" }

        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        val key = annotationProcessor.resolveKey(annotation, method, args, parameterNames)

        log.debug("Acquiring distributed lock with key: '{}' for method: {}", key, method.name)

        return try {
            lockProvider.withLock(
                key = key,
                waitTime = annotation.timeUnit.toMillis(annotation.waitTime),
                leaseTime = annotation.timeUnit.toMillis(annotation.leaseTime)
            ) {
                log.debug("Successfully acquired lock: '{}'. Executing method: {}", key, method.name)
                joinPoint.proceed()
            }
        } catch (exception: Exception) {
            log.error("Error while processing distributed lock '{}' for method: {}", key, method.name, exception)
            throw exception
        } finally {
            log.debug("Released distributed lock: '{}'", key)
        }
    }
}
