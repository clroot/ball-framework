package io.clroot.ball.domain.port

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@Suppress("RedundantNullableReturnType")
class SearchCriteriaTest :
    FunSpec({

        context("SearchCriteria isEmpty()") {
            test("모든 nullable 필드가 null인 경우 true를 반환해야 한다") {
                val criteria =
                    object : SearchCriteria {
                        val name: String? = null
                        val age: Int? = null
                        val active: Boolean? = null
                    }

                criteria.isEmpty() shouldBe true
            }

            test("일부 nullable 필드에 값이 있는 경우 false를 반환해야 한다") {
                val criteria =
                    object : SearchCriteria {
                        val name: String? = "John"
                        val age: Int? = null
                        val active: Boolean? = null
                    }

                criteria.isEmpty() shouldBe false
            }

            test("모든 nullable 필드에 값이 있는 경우 false를 반환해야 한다") {
                val criteria =
                    object : SearchCriteria {
                        val name: String? = "John"
                        val age: Int? = 25
                        val active: Boolean? = true
                    }

                criteria.isEmpty() shouldBe false
            }

            test("non-nullable 필드는 isEmpty 판단에 영향을 주지 않아야 한다") {
                val criteria =
                    object : SearchCriteria {
                        val id: String = "123"
                        val name: String? = null
                        val age: Int? = null
                    }

                criteria.isEmpty() shouldBe true
            }

            test("중첩된 SearchCriteria가 모두 비어있는 경우 true를 반환해야 한다") {
                val nestedCriteria =
                    object : SearchCriteria {
                        val street: String? = null
                        val city: String? = null
                    }

                val criteria =
                    object : SearchCriteria {
                        val name: String? = null
                        val address: SearchCriteria? = nestedCriteria
                    }

                criteria.isEmpty() shouldBe true
            }

            test("중첩된 SearchCriteria에 값이 있는 경우 false를 반환해야 한다") {
                val nestedCriteria =
                    object : SearchCriteria {
                        val street: String? = "123 Main St"
                        val city: String? = null
                    }

                val criteria =
                    object : SearchCriteria {
                        val name: String? = null
                        val address: SearchCriteria? = nestedCriteria
                    }

                criteria.isEmpty() shouldBe false
            }

            test("중첩된 SearchCriteria가 null인 경우 true를 반환해야 한다") {
                val criteria =
                    object : SearchCriteria {
                        val name: String? = null
                        val address: SearchCriteria? = null
                    }

                criteria.isEmpty() shouldBe true
            }

            test("복잡한 중첩 구조에서도 올바르게 동작해야 한다") {
                val deepNestedCriteria =
                    object : SearchCriteria {
                        val zipCode: String? = "12345"
                    }

                val nestedCriteria =
                    object : SearchCriteria {
                        val street: String? = null
                        val city: String? = null
                        val details: SearchCriteria? = deepNestedCriteria
                    }

                val criteria =
                    object : SearchCriteria {
                        val name: String? = null
                        val age: Int? = null
                        val address: SearchCriteria? = nestedCriteria
                    }

                criteria.isEmpty() shouldBe false
            }

            test("빈 SearchCriteria는 true를 반환해야 한다") {
                val criteria = object : SearchCriteria {}

                criteria.isEmpty() shouldBe true
            }

            test("Collection 타입의 nullable 필드도 올바르게 처리해야 한다") {
                val criteria =
                    object : SearchCriteria {
                        val tags: List<String>? = null
                        val categories: Set<String>? = emptySet()
                    }

                criteria.isEmpty() shouldBe false
            }
        }

        context("SearchCriteria 실제 사용 사례") {
            test("사용자 검색 조건") {
                data class UserSearchCriteria(
                    val name: String? = null,
                    val email: String? = null,
                    val minAge: Int? = null,
                    val maxAge: Int? = null,
                    val isActive: Boolean? = null,
                ) : SearchCriteria

                val emptyCriteria = UserSearchCriteria()
                emptyCriteria.isEmpty() shouldBe true

                val partialCriteria = UserSearchCriteria(name = "John")
                partialCriteria.isEmpty() shouldBe false

                val fullCriteria =
                    UserSearchCriteria(
                        name = "John",
                        email = "john@example.com",
                        minAge = 18,
                        maxAge = 65,
                        isActive = true,
                    )
                fullCriteria.isEmpty() shouldBe false
            }

            test("주문 검색 조건 with 중첩") {
                data class DateRangeCriteria(
                    val startDate: String? = null,
                    val endDate: String? = null,
                ) : SearchCriteria

                data class OrderSearchCriteria(
                    val orderId: String? = null,
                    val customerId: String? = null,
                    val minAmount: Double? = null,
                    val maxAmount: Double? = null,
                    val dateRange: DateRangeCriteria? = null,
                ) : SearchCriteria

                val emptyCriteria = OrderSearchCriteria()
                emptyCriteria.isEmpty() shouldBe true

                val criteriaWithEmptyDateRange =
                    OrderSearchCriteria(
                        orderId = null,
                        dateRange = DateRangeCriteria(),
                    )
                criteriaWithEmptyDateRange.isEmpty() shouldBe true

                val criteriaWithDateRange =
                    OrderSearchCriteria(
                        orderId = null,
                        dateRange = DateRangeCriteria(startDate = "2024-01-01"),
                    )
                criteriaWithDateRange.isEmpty() shouldBe false
            }
        }
    })
