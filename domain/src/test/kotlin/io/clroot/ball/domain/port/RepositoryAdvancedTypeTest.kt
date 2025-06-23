package io.clroot.ball.domain.port

import io.clroot.ball.domain.exception.BusinessRuleException
import io.clroot.ball.domain.model.AggregateRoot
import io.clroot.ball.domain.model.EntityBase
import io.clroot.ball.domain.model.vo.BinaryId
import java.time.LocalDateTime

// 복잡한 시나리오 테스트

// 1. 다양한 ID 타입을 사용하는 경우
data class StringId(
    val value: String,
)

data class LongId(
    val value: Long,
)

// 2. EntityBase만 상속하는 경우
class SimpleEntity(
    id: BinaryId,
    val data: String,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) : EntityBase<BinaryId>(id, createdAt, updatedAt)

// 3. 복잡한 도메인 모델
class Order(
    id: BinaryId,
    val customerName: String,
    private var status: OrderStatus = OrderStatus.PENDING,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    deletedAt: LocalDateTime? = null,
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt, 0) {
    enum class OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED }

    fun confirm() {
        if (status != OrderStatus.PENDING) {
            throw BusinessRuleException("Can only confirm pending orders")
        }
        status = OrderStatus.CONFIRMED
    }
}

// Repository 정의들
interface SimpleEntityRepository : Repository<SimpleEntity, BinaryId>

interface OrderRepository : Repository<Order, BinaryId>

// 제네릭 경계 테스트
class GenericRepositoryTest {
    // 이 함수는 EntityBase를 상속하는 모든 타입에 대해 작동해야 함
    fun <T : EntityBase<ID>, ID : Any> genericSave(
        repository: Repository<T, ID>,
        entity: T,
    ): T = repository.save(entity)

    // AggregateRoot 전용 함수
    fun <T : AggregateRoot<ID>, ID : Any> saveWithEvents(
        repository: Repository<T, ID>, // 여기서 타입 에러가 발생할 수 있음
        entity: T,
    ): T {
        val saved = repository.save(entity)
        // 이벤트 처리 로직
        entity.clearEvents()
        return saved
    }

    fun testVariousScenarios() {
        // SimpleEntity 테스트
        val simpleRepo =
            object : SimpleEntityRepository {
                override fun findById(id: BinaryId): SimpleEntity? = null

                override fun findAll(): List<SimpleEntity> = emptyList()

                override fun save(entity: SimpleEntity): SimpleEntity = entity

                override fun delete(id: BinaryId) {}
            }

        // Order (AggregateRoot) 테스트
        val orderRepo =
            object : OrderRepository {
                override fun findById(id: BinaryId): Order? = null

                override fun findAll(): List<Order> = emptyList()

                override fun save(entity: Order): Order = entity

                override fun delete(id: BinaryId) {}
            }

        val simpleEntity = SimpleEntity(BinaryId.new(), "test")
        val order = Order(BinaryId.new(), "customer")

        // EntityBase 타입으로 처리
        genericSave(simpleRepo, simpleEntity)
        genericSave(orderRepo, order) // Order는 AggregateRoot이고 AggregateRoot는 EntityBase를 상속

        // AggregateRoot 전용 처리
        // saveWithEvents(simpleRepo, simpleEntity) // 컴파일 에러 - SimpleEntity는 AggregateRoot가 아님
        saveWithEvents(orderRepo, order) // 정상 작동해야 함
    }
}

// 공변성(Covariance) 관련 테스트
class CovarianceTest {
    // Repository의 타입 파라미터가 공변적인지 테스트
    fun testCovariance() {
        val orderRepo: OrderRepository =
            object : OrderRepository {
                override fun findById(id: BinaryId): Order? = null

                override fun findAll(): List<Order> = emptyList()

                override fun save(entity: Order): Order = entity

                override fun delete(id: BinaryId) {}
            }

        // Order는 AggregateRoot이고, AggregateRoot는 EntityBase를 상속하므로
        // 이론적으로는 Repository<Order, BinaryId>를 Repository<EntityBase<BinaryId>, BinaryId>로
        // 사용할 수 있어야 하지만, Kotlin의 타입 시스템에서는 불가능

        // val baseRepo: Repository<EntityBase<BinaryId>, BinaryId> = orderRepo // 컴파일 에러

        // 이것이 문제의 핵심일 수 있음
    }
}

// 타입 추론 관련 문제
class TypeInferenceTest {
    // 복잡한 제네릭 경계에서 타입 추론이 실패할 수 있음
    inline fun <reified T : EntityBase<ID>, ID : Any> findAndProcess(
        repository: Repository<T, ID>,
        id: ID,
        processor: (T?) -> Unit,
    ) {
        val entity = repository.findById(id)
        processor(entity)
    }

    fun testTypeInference() {
        val orderRepo: OrderRepository =
            object : OrderRepository {
                override fun findById(id: BinaryId): Order? = null

                override fun findAll(): List<Order> = emptyList()

                override fun save(entity: Order): Order = entity

                override fun delete(id: BinaryId) {}
            }

        val id = BinaryId.new()

        // 타입 추론이 제대로 작동하는지 테스트
        findAndProcess(orderRepo, id) { order ->
            // order의 타입이 Order?로 추론되어야 함
            order?.confirm()
        }
    }
}
