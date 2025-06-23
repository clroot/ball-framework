package io.clroot.ball.domain.port

import io.clroot.ball.domain.model.AggregateRoot
import io.clroot.ball.domain.model.EntityBase
import io.clroot.ball.domain.model.vo.BinaryId
import java.time.LocalDateTime

/**
 * Repository 사용 시 발생할 수 있는 잠재적 문제들을 테스트
 */
class RepositoryPotentialIssuesTest {
    // 테스트용 엔티티들
    class Product(
        id: BinaryId,
        val name: String,
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now(),
        deletedAt: LocalDateTime? = null,
    ) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt, 0)

    interface ProductRepository : Repository<Product, BinaryId>

    /**
     * 문제 1: Repository<T : EntityBase<ID>, ID>에서 T가 AggregateRoot인 경우
     *
     * Kotlin의 타입 시스템에서 Repository<AggregateRoot<ID>, ID>는
     * Repository<EntityBase<ID>, ID>의 서브타입이 아님 (타입 파라미터는 불변)
     */
    fun issue1TypeParameterInvariance() {
        val productRepo: ProductRepository =
            object : ProductRepository {
                override fun findById(id: BinaryId): Product? = null

                override fun findAll(): List<Product> = emptyList()

                override fun save(entity: Product): Product = entity

                override fun delete(id: BinaryId) {}
            }

        // 컴파일 에러: Type mismatch
        // val entityRepo: Repository<EntityBase<BinaryId>, BinaryId> = productRepo

        // 이 문제는 실제로는 발생하지 않음. Repository의 타입 파라미터가
        // T : EntityBase<ID>로 선언되어 있어서 AggregateRoot도 허용됨
    }

    /**
     * 문제 2: 제네릭 메서드에서 타입 추론 문제
     */
    fun issue2GenericTypeInference() {
        // 제네릭 헬퍼 함수
        fun <T : EntityBase<ID>, ID : Any> processRepository(
            repo: Repository<T, ID>,
            id: ID,
        ): T? = repo.findById(id)

        val productRepo: ProductRepository =
            object : ProductRepository {
                override fun findById(id: BinaryId): Product = Product(id, "Test Product")

                override fun findAll(): List<Product> = emptyList()

                override fun save(entity: Product): Product = entity

                override fun delete(id: BinaryId) {}
            }

        val id = BinaryId.generate()

        // 타입 추론이 제대로 작동함
        val product: Product? = processRepository(productRepo, id)

        // 명시적 타입 지정도 가능
        val product2 = processRepository<Product, BinaryId>(productRepo, id)
    }

    /**
     * 문제 3: Star Projection 사용 시 문제
     */
    fun issue3StarProjection() {
        val repositories = mutableListOf<Repository<*, *>>()

        val productRepo: ProductRepository =
            object : ProductRepository {
                override fun findById(id: BinaryId): Product? = null

                override fun findAll(): List<Product> = emptyList()

                override fun save(entity: Product): Product = entity

                override fun delete(id: BinaryId) {}
            }

        repositories.add(productRepo) // 문제 없음

        // 하지만 사용할 때는 타입 정보가 손실됨
        val repo = repositories[0]
        // val product = repo.findById(BinaryId.new()) // 컴파일 에러: Type mismatch
    }

    /**
     * 문제 4: 다중 상속 경계 (Multiple Bounds)
     */
    fun issue4MultipleBounds() {
        // AggregateRoot만 처리하는 특별한 함수
        fun <T, ID : Any> processAggregateRepository(repo: Repository<T, ID>): T? where T : AggregateRoot<ID> {
            // T는 AggregateRoot이면서 EntityBase여야 함
            // AggregateRoot가 이미 EntityBase를 상속하므로 중복됨
            return repo.findById(null as ID)
        }

        // 이런 경우는 실제로 문제가 되지 않음
    }

    /**
     * 실제로 발생할 수 있는 문제: 공변/반공변 관련
     */
    interface ReadOnlyRepository<out T : EntityBase<ID>, ID : Any> {
        fun findById(id: ID): T?

        fun findAll(): List<T>
    }

    fun realIssueVarianceProblems() {
        // out 키워드로 공변성을 추가하면 서브타입 관계가 성립
        val productRepo: ReadOnlyRepository<Product, BinaryId> =
            object : ReadOnlyRepository<Product, BinaryId> {
                override fun findById(id: BinaryId): Product? = null

                override fun findAll(): List<Product> = emptyList()
            }

        // 공변성 덕분에 가능
        val entityRepo: ReadOnlyRepository<EntityBase<BinaryId>, BinaryId> = productRepo

        // 하지만 Ball Framework의 Repository는 save/update 메서드 때문에
        // 공변성을 추가할 수 없음 (T를 매개변수로 받기 때문)
    }

    /**
     * 실제 사용 시나리오에서의 베스트 프랙티스
     */
    fun bestPractices() {
        // 1. 구체적인 타입 사용
        val productRepo: ProductRepository =
            object : ProductRepository {
                override fun findById(id: BinaryId): Product? = null

                override fun findAll(): List<Product> = emptyList()

                override fun save(entity: Product): Product = entity

                override fun delete(id: BinaryId) {}
            }

        // 2. 제네릭 함수에서는 상한 경계 사용
        fun <T : EntityBase<ID>, ID : Any> genericOperation(
            repo: Repository<T, ID>,
            entity: T,
        ): T = repo.save(entity)

        // 3. AggregateRoot 전용 처리가 필요한 경우
        fun <T : AggregateRoot<ID>, ID : Any> saveAndPublishEvents(
            repo: Repository<T, ID>,
            entity: T,
        ): T {
            val saved = repo.save(entity)
            // 이벤트 발행 로직
            entity.domainEvents.forEach { event ->
                // 이벤트 처리
            }
            entity.clearEvents()
            return saved
        }
    }
}
