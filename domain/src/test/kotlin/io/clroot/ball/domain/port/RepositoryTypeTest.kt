package io.clroot.ball.domain.port

import io.clroot.ball.domain.model.AggregateRoot
import io.clroot.ball.domain.model.vo.BinaryId
import java.time.LocalDateTime

// 테스트용 AggregateRoot 구현
class TestUser(
    id: BinaryId,
    val name: String,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, 0) {
    fun changeName(newName: String) {
        // 비즈니스 로직
    }
}

// Repository 인터페이스 정의 - 이 부분에서 타입 오류가 발생할 수 있음
interface TestUserRepository : Repository<TestUser, BinaryId>

// 구체적인 Repository 구현
class TestUserRepositoryImpl : TestUserRepository {
    private val storage = mutableMapOf<BinaryId, TestUser>()

    override fun findById(id: BinaryId): TestUser? = storage[id]

    override fun findAll(): List<TestUser> = storage.values.toList()

    override fun save(entity: TestUser): TestUser {
        storage[entity.id] = entity
        return entity
    }

    override fun delete(id: BinaryId) {
        storage.remove(id)
    }
}

// 다양한 사용 패턴 테스트
class RepositoryTypeUsageTest {
    fun testBasicUsage() {
        val repository: TestUserRepository = TestUserRepositoryImpl()
        val user = TestUser(BinaryId.generate(), "John")

        // 저장
        repository.save(user)

        // 조회
        val found = repository.findById(user.id)

        // 업데이트
        user.changeName("Jane")
        repository.save(user)

        // 삭제
        repository.delete(user)
    }

    // 제네릭 타입 파라미터를 사용한 함수
    fun <T : AggregateRoot<ID>, ID : Any> processEntity(
        repository: Repository<T, ID>,
        id: ID,
    ): T? = repository.findById(id)

    // 이 부분에서 타입 추론 문제가 발생할 수 있음
    fun testGenericUsage() {
        val repository: TestUserRepository = TestUserRepositoryImpl()
        val user = TestUser(BinaryId.generate(), "John")
        repository.save(user)

        // 타입 추론 테스트
        val result = processEntity(repository, user.id)
    }
}
