package io.clroot.ball.application.usecase

import arrow.core.Either
import io.clroot.ball.application.ApplicationError
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.springframework.context.ApplicationEventPublisher

class ParallelUseCaseCompositionTest : BehaviorSpec({
    val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    given("병렬 UseCase 조합") {
        val useCase1 = ParallelTestUseCase1(applicationEventPublisher)
        val useCase2 = ParallelTestUseCase2(applicationEventPublisher)
        val useCase3 = ParallelTestUseCase3(applicationEventPublisher)
        val composition = TestParallelUseCaseComposition(
            applicationEventPublisher, useCase1, useCase2, useCase3
        )

        `when`("모든 UseCase가 성공할 때") {
            val command = ParallelCompositionCommand(input = "test")
            val result = composition.execute(command)

            then("모든 결과가 병합되어야 한다") {
                result.shouldBeInstanceOf<Either.Right<ParallelCompositionResult>>()
                val finalResult = result.getOrNull()!!
                finalResult.result1 shouldBe "test-1"
                finalResult.result2 shouldBe "test-2"
                finalResult.result3 shouldBe "test-3"
            }
        }

        `when`("하나의 UseCase가 실패할 때") {
            val command = ParallelCompositionCommand(input = "fail")
            val result = composition.execute(command)

            then("전체 조합이 실패해야 한다") {
                result.shouldBeInstanceOf<Either.Left<ApplicationError>>()
            }
        }
    }
})

// Test Data Classes
data class ParallelCompositionCommand(val input: String)
data class ParallelResult1(val value: String)
data class ParallelResult2(val value: String)
data class ParallelResult3(val value: String)
data class ParallelCompositionResult(
    val result1: String,
    val result2: String,
    val result3: String
)

// Test UseCases
class ParallelTestUseCase1(applicationEventPublisher: ApplicationEventPublisher) : 
    UseCase<String, ParallelResult1>(applicationEventPublisher) {
    
    override fun executeInternal(command: String): ParallelResult1 {
        if (command == "fail") {
            throw RuntimeException("UseCase1 failed")
        }
        // Thread.sleep 제거 - 실제 환경에서는 불필요
        return ParallelResult1("$command-1")
    }
}

class ParallelTestUseCase2(applicationEventPublisher: ApplicationEventPublisher) : 
    UseCase<String, ParallelResult2>(applicationEventPublisher) {
    
    override fun executeInternal(command: String): ParallelResult2 {
        if (command == "fail") {
            throw RuntimeException("UseCase2 failed")
        }
        return ParallelResult2("$command-2")
    }
}

class ParallelTestUseCase3(applicationEventPublisher: ApplicationEventPublisher) : 
    UseCase<String, ParallelResult3>(applicationEventPublisher) {
    
    override fun executeInternal(command: String): ParallelResult3 {
        return ParallelResult3("$command-3")
    }
}

// Test Parallel Composition
class TestParallelUseCaseComposition(
    applicationEventPublisher: ApplicationEventPublisher,
    private val useCase1: ParallelTestUseCase1,
    private val useCase2: ParallelTestUseCase2,
    private val useCase3: ParallelTestUseCase3
) : ParallelUseCaseComposition<ParallelCompositionCommand, ParallelCompositionResult>(applicationEventPublisher) {

    override fun executeParallel(command: ParallelCompositionCommand): Either<ApplicationError, ParallelCompositionResult> {
        return runInParallel(
            { useCase1.execute(command.input) },
            { useCase2.execute(command.input) },
            { useCase3.execute(command.input) }
        ).map { (result1, result2, result3) ->
            ParallelCompositionResult(
                result1 = result1.value,
                result2 = result2.value,
                result3 = result3.value
            )
        }
    }
}