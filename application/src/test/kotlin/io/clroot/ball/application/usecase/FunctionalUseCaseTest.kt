package io.clroot.ball.application.usecase
import arrow.core.Either
import arrow.core.raise.Raise
import io.clroot.ball.application.ApplicationError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.context.ApplicationEventPublisher

class FunctionalUseCaseTest :
    FunSpec({
        val publisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val useCase = TestFunctionalUseCase(publisher)

        test("성공 시 Right를 반환한다") {
            val result = useCase.execute(1)

            result shouldBe Either.Right("value:1")
        }

        test("사용자 정의 오류가 ApplicationError로 매핑된다") {
            val result = useCase.execute(0)

            result shouldBe Either.Left(ApplicationError.NotFound)
        }
    })

private class TestFunctionalUseCase(
    applicationEventPublisher: ApplicationEventPublisher,
) : FunctionalUseCase<Int, String, SampleError>(applicationEventPublisher) {
    override fun Raise<SampleError>.executeFunctional(command: Int): String =
        if (command <= 0) {
            raise(SampleError.Missing)
        } else {
            "value:$command"
        }

    override fun mapError(error: SampleError): ApplicationError =
        when (error) {
            SampleError.Missing -> ApplicationError.NotFound
        }
}

private sealed interface SampleError {
    data object Missing : SampleError
}
