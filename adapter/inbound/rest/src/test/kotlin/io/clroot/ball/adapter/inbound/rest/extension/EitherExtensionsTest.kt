package io.clroot.ball.adapter.inbound.rest.extension

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.clroot.ball.adapter.outbound.data.access.core.exception.EntityNotFoundException
import io.clroot.ball.application.ApplicationError
import io.clroot.ball.domain.exception.BusinessRuleException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus

class EitherExtensionsTest :
    DescribeSpec({

        describe("Either<ApplicationError, T>.toResponseEntity()") {

            context("성공 케이스") {
                it("Right 값이 있을 때 200 OK 응답을 반환해야 한다") {
                    // given
                    val successData = "Success Result"
                    val either: Either<ApplicationError, String> = successData.right()

                    // when
                    val response = either.toResponseEntity()

                    // then
                    response.statusCode shouldBe HttpStatus.OK
                    response.body shouldBe successData
                }

                it("Right에 객체가 있을 때 올바른 응답을 반환해야 한다") {
                    // given
                    data class TestDto(
                        val id: Long,
                        val name: String,
                    )
                    val testDto = TestDto(1L, "Test")
                    val either: Either<ApplicationError, TestDto> = testDto.right()

                    // when
                    val response = either.toResponseEntity()

                    // then
                    response.statusCode shouldBe HttpStatus.OK
                    response.body shouldBe testDto
                }
            }

            context("에러 케이스") {
                it("DomainError일 때 해당 도메인 예외를 던져야 한다") {
                    // given
                    val domainException = BusinessRuleException("Invalid input")
                    val applicationError = ApplicationError.DomainError(domainException)
                    val either: Either<ApplicationError, String> = applicationError.left()

                    // when & then
                    val thrown =
                        shouldThrow<BusinessRuleException> {
                            either.toResponseEntity()
                        }
                    thrown.message shouldBe "Invalid input"
                }

                it("NotFound일 때 EntityNotFoundException을 던져야 한다") {
                    // given
                    val either: Either<ApplicationError, String> = ApplicationError.NotFound.left()

                    // when & then
                    shouldThrow<EntityNotFoundException> {
                        either.toResponseEntity()
                    }
                }

                it("SystemError일 때 RuntimeException을 던져야 한다") {
                    // given
                    val cause = IllegalStateException("Original cause")
                    val systemError = ApplicationError.SystemError("System failure", cause)
                    val either: Either<ApplicationError, String> = systemError.left()

                    // when & then
                    val thrown =
                        shouldThrow<RuntimeException> {
                            either.toResponseEntity()
                        }
                    thrown.message shouldBe "System failure"
                    thrown.cause shouldBe cause
                }
            }
        }

        describe("Either<ApplicationError, T?>.toResponseEntityWithNull()") {

            context("성공 케이스") {
                it("Right에 non-null 값이 있을 때 200 OK 응답을 반환해야 한다") {
                    // given
                    val successData = "Success Result"
                    val either: Either<ApplicationError, String?> = successData.right()

                    // when
                    val response = either.toResponseEntityWithNull()

                    // then
                    response.statusCode shouldBe HttpStatus.OK
                    response.body shouldBe successData
                }

                it("Right에 null 값이 있을 때 EntityNotFoundException을 던져야 한다") {
                    // given
                    val either: Either<ApplicationError, String?> = (null as String?).right()

                    // when & then
                    val thrown =
                        shouldThrow<EntityNotFoundException> {
                            either.toResponseEntityWithNull()
                        }
                    thrown.message shouldBe "Resource not found"
                }
            }

            context("에러 케이스") {
                it("Left에 에러가 있을 때 해당 예외를 던져야 한다") {
                    // given
                    val domainException = BusinessRuleException("Business rule violated")
                    val applicationError = ApplicationError.DomainError(domainException)
                    val either: Either<ApplicationError, String?> = applicationError.left()

                    // when & then
                    val thrown =
                        shouldThrow<BusinessRuleException> {
                            either.toResponseEntityWithNull()
                        }
                    thrown.message shouldBe "Business rule violated"
                }
            }
        }

        describe("ApplicationError.toException() private 함수 동작") {

            it("DomainError는 원래 도메인 예외로 변환되어야 한다") {
                // given
                val validationException = BusinessRuleException("Validation failed")
                val domainError = ApplicationError.DomainError(validationException)
                val either: Either<ApplicationError, String> = domainError.left()

                // when & then
                val thrown =
                    shouldThrow<BusinessRuleException> {
                        either.toResponseEntity()
                    }
                thrown shouldBe validationException
            }

            it("NotFound는 EntityNotFoundException으로 변환되어야 한다") {
                // given
                val either: Either<ApplicationError, String> = ApplicationError.NotFound.left()

                // when & then
                val thrown =
                    shouldThrow<EntityNotFoundException> {
                        either.toResponseEntity()
                    }
                thrown.message shouldBe "Resource not found"
            }

            it("SystemError는 RuntimeException으로 변환되어야 한다") {
                // given
                val originalCause = Exception("Original")
                val systemError = ApplicationError.SystemError("System error", originalCause)
                val either: Either<ApplicationError, String> = systemError.left()

                // when & then
                val thrown =
                    shouldThrow<RuntimeException> {
                        either.toResponseEntity()
                    }
                thrown.message shouldBe "System error"
                thrown.cause shouldBe originalCause
            }

            it("SystemError with null cause도 처리할 수 있어야 한다") {
                // given
                val systemError = ApplicationError.SystemError("System error", null)
                val either: Either<ApplicationError, String> = systemError.left()

                // when & then
                val thrown =
                    shouldThrow<RuntimeException> {
                        either.toResponseEntity()
                    }
                thrown.message shouldBe "System error"
                thrown.cause shouldBe null
            }
        }

        describe("복합 시나리오") {
            it("체이닝된 Either 연산과 함께 동작해야 한다") {
                // given
                val either =
                    "initial"
                        .right()
                        .map { it.uppercase() }
                        .map { "${it}_PROCESSED" }

                // when
                val response = either.toResponseEntity()

                // then
                response.statusCode shouldBe HttpStatus.OK
                response.body shouldBe "INITIAL_PROCESSED"
            }

            it("Optional한 데이터 처리 시나리오") {
                // given
                data class User(
                    val id: Long,
                    val name: String,
                )
                val user = User(1L, "John")
                val either: Either<ApplicationError, User?> = user.right()

                // when
                val response = either.toResponseEntityWithNull()

                // then
                response.statusCode shouldBe HttpStatus.OK
                response.body shouldBe user
            }
        }
    })
