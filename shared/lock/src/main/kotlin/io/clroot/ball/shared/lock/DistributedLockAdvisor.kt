package io.clroot.ball.shared.lock

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

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

        val key = annotationProcessor.resolveKey(annotation, args, parameterNames)

        log.debug("Acquiring distributed lock: {}", key)

        return lockProvider.withLock(
            key = key,
            waitTime = annotation.timeUnit.toMillis(annotation.waitTime),
            leaseTime = annotation.timeUnit.toMillis(annotation.leaseTime)
        ) {
            joinPoint.proceed()
        }
    }
}
