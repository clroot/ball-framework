package io.clroot.ball.application.warmup

import io.clroot.ball.domain.slf4j
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * warm-up을 관리하는 서비스
 *
 * 이 서비스는 등록된 모든 warm-up 작업을 병렬로 실행한다.
 */
@Service
class WarmupService(
    private val applicationContext: ApplicationContext,
) {
    private val log = slf4j()
    private val warmupResults = ConcurrentHashMap<String, WarmupResult>()

    companion object {
        private const val DEFAULT_TIMEOUT_SECONDS = 30L
    }

    /**
     * 모든 warm-up 작업 실행
     *
     * @param timeoutSeconds warm-up 전체 타임아웃 (기본: 30초)
     * @return warm-up 결과 맵
     */
    fun performWarmup(timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS): Map<String, WarmupResult> {
        log.info("warm-up 시작...")

        val warmupTasks = applicationContext.getBeansOfType(WarmupTask::class.java)

        if (warmupTasks.isEmpty()) {
            log.info("등록된 warm-up 작업이 없습니다.")
            return emptyMap()
        }

        val futures =
            warmupTasks.map { (beanName, task) ->
                CompletableFuture.supplyAsync {
                    val startTime = System.currentTimeMillis()
                    try {
                        task.warmup()
                        val duration = System.currentTimeMillis() - startTime
                        val result =
                            WarmupResult(
                                taskName = beanName,
                                success = true,
                                durationMs = duration,
                                error = null,
                            )
                        warmupResults[beanName] = result
                        log.debug("{} warm-up 성공 ({}ms)", beanName, duration)
                        result
                    } catch (e: Exception) {
                        val duration = System.currentTimeMillis() - startTime
                        val result =
                            WarmupResult(
                                taskName = beanName,
                                success = false,
                                durationMs = duration,
                                error = e.message,
                            )
                        warmupResults[beanName] = result
                        log.warn("{} warm-up 실패: {}", beanName, e.message)
                        result
                    }
                }
            }

        try {
            CompletableFuture
                .allOf(*futures.toTypedArray())
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .join()
        } catch (e: Exception) {
            log.error("warm-up 타임아웃 또는 오류 발생", e)
        }

        val successCount = warmupResults.values.count { it.success }
        val totalDuration = warmupResults.values.sumOf { it.durationMs }

        log.info(
            "warm-up 완료: {}/{} 성공, 총 소요시간: {}ms",
            successCount,
            warmupResults.size,
            totalDuration,
        )

        return warmupResults.toMap()
    }

    /**
     * 특정 모듈의 warm-up 상태 조회
     */
    fun getWarmupStatus(taskName: String): WarmupResult? = warmupResults[taskName]

    /**
     * 모든 warm-up 상태 조회
     */
    fun getAllWarmupStatus(): Map<String, WarmupResult> = warmupResults.toMap()
}
