package io.clroot.ball.adapter.inbound.rest.monitoring

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.lang.annotation.Inherited

/**
 * 성능 모니터링 어노테이션
 * 
 * 이 어노테이션은 메서드의 실행 시간을 측정하고 로깅하는 데 사용됩니다.
 * 
 * @property threshold 경고 임계값 (밀리초) - 이 값을 초과하면 경고 로그가 출력됩니다.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
annotation class Monitored(val threshold: Long = 1000)

/**
 * 성능 모니터링 애스펙트
 * 
 * 이 클래스는 @Monitored 어노테이션이 적용된 메서드의 실행 시간을 측정하고 로깅합니다.
 */
@Aspect
@Component
class PerformanceMonitoringAspect {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 컨트롤러 메서드 포인트컷
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    fun controllerMethods() {}

    /**
     * @Monitored 어노테이션이 적용된 메서드 포인트컷
     */
    @Pointcut("@annotation(io.clroot.ball.adapter.inbound.rest.monitoring.Monitored)")
    fun monitoredMethods() {}

    /**
     * @Monitored 어노테이션이 적용된 클래스의 메서드 포인트컷
     */
    @Pointcut("@within(io.clroot.ball.adapter.inbound.rest.monitoring.Monitored)")
    fun monitoredClassMethods() {}

    /**
     * 컨트롤러 메서드 실행 시간 측정
     */
    @Around("controllerMethods()")
    fun monitorControllerPerformance(joinPoint: ProceedingJoinPoint): Any {
        return monitorPerformance(joinPoint, 1000) // 컨트롤러 메서드의 기본 임계값은 1000ms
    }

    /**
     * @Monitored 어노테이션이 적용된 메서드 실행 시간 측정
     */
    @Around("monitoredMethods() || monitoredClassMethods()")
    fun monitorAnnotatedMethodPerformance(joinPoint: ProceedingJoinPoint): Any {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        
        // 메서드에 @Monitored 어노테이션이 있는지 확인
        val methodAnnotation = method.getAnnotation(Monitored::class.java)
        if (methodAnnotation != null) {
            return monitorPerformance(joinPoint, methodAnnotation.threshold)
        }
        
        // 클래스에 @Monitored 어노테이션이 있는지 확인
        val classAnnotation = method.declaringClass.getAnnotation(Monitored::class.java)
        if (classAnnotation != null) {
            return monitorPerformance(joinPoint, classAnnotation.threshold)
        }
        
        // 어노테이션이 없는 경우 기본 임계값 사용
        return monitorPerformance(joinPoint, 1000)
    }

    /**
     * 메서드 실행 시간 측정 및 로깅
     */
    private fun monitorPerformance(joinPoint: ProceedingJoinPoint, threshold: Long): Any {
        val signature = joinPoint.signature as MethodSignature
        val className = signature.declaringType.simpleName
        val methodName = signature.name
        
        val stopWatch = StopWatch("$className.$methodName")
        stopWatch.start()
        
        try {
            return joinPoint.proceed()
        } finally {
            stopWatch.stop()
            val executionTime = stopWatch.totalTimeMillis
            
            if (executionTime > threshold) {
                log.warn("Performance warning: {}.{} took {}ms (threshold: {}ms)",
                    className, methodName, executionTime, threshold)
            } else {
                log.debug("Performance: {}.{} took {}ms", className, methodName, executionTime)
            }
            
            // 메트릭 수집 (예: Micrometer, Prometheus 등)
            collectMetrics(className, methodName, executionTime)
        }
    }

    /**
     * 성능 메트릭 수집
     */
    private fun collectMetrics(className: String, methodName: String, executionTime: Long) {
        // 여기에 메트릭 수집 코드 추가 (예: Micrometer, Prometheus 등)
        // 현재는 로깅만 수행
        log.trace("Metric: method.execution.time,class={},method={} value={}ms",
            className, methodName, executionTime)
    }
}