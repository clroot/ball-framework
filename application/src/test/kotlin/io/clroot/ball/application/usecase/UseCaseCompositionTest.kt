package io.clroot.ball.application.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.clroot.ball.application.ApplicationError
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.springframework.context.ApplicationEventPublisher

class UseCaseCompositionTest : BehaviorSpec({
    val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    given("UseCase 조합") {
        val useCase1 = TestUseCase1(applicationEventPublisher)
        val useCase2 = TestUseCase2(applicationEventPublisher)
        val composition = TestUseCaseComposition(applicationEventPublisher, useCase1, useCase2)

        `when`("모든 UseCase가 성공할 때") {
            val command = CompositionCommand(input = "test")
            val result = composition.execute(command)

            then("최종 결과가 성공해야 한다") {
                result.shouldBeInstanceOf<Either.Right<CompositionResult>>()
                result.getOrNull()?.finalValue shouldBe "test-processed-transformed"
            }
        }

        `when`("첫 번째 UseCase가 실패할 때") {
            val command = CompositionCommand(input = "fail1")
            val result = composition.execute(command)

            then("전체 조합이 실패해야 한다") {
                result.shouldBeInstanceOf<Either.Left<ApplicationError>>()
            }
        }

        `when`("두 번째 UseCase가 실패할 때") {
            val command = CompositionCommand(input = "fail2")
            val result = composition.execute(command)

            then("전체 조합이 실패해야 한다") {
                result.shouldBeInstanceOf<Either.Left<ApplicationError>>()
            }
        }
    }
})

// Test Data Classes
data class CompositionCommand(val input: String)
data class IntermediateResult(val value: String)
data class CompositionResult(val finalValue: String)

// Test UseCase 1
class TestUseCase1(applicationEventPublisher: ApplicationEventPublisher) : 
    UseCase<String, IntermediateResult>(applicationEventPublisher) {
    
    override fun executeInternal(command: String): IntermediateResult {
        if (command == "fail1") {
            throw RuntimeException("UseCase1 failed")
        }
        return IntermediateResult("$command-processed")
    }
}

// Test UseCase 2
class TestUseCase2(applicationEventPublisher: ApplicationEventPublisher) : 
    UseCase<IntermediateResult, CompositionResult>(applicationEventPublisher) {
    
    override fun executeInternal(command: IntermediateResult): CompositionResult {
        if (command.value.contains("fail2")) {
            throw RuntimeException("UseCase2 failed")
        }
        return CompositionResult("${command.value}-transformed")
    }
}

// Test Composition
class TestUseCaseComposition(
    applicationEventPublisher: ApplicationEventPublisher,
    private val useCase1: TestUseCase1,
    private val useCase2: TestUseCase2
) : UseCaseComposition<CompositionCommand, CompositionResult>(applicationEventPublisher) {

    override fun executeComposition(command: CompositionCommand): Either<ApplicationError, CompositionResult> {
        return useCase1.execute(command.input)
            .then { intermediateResult -> 
                useCase2.execute(intermediateResult)
            }
    }
}