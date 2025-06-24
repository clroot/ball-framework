package io.clroot.ball.adapter.outbound.data.access.jpa

import io.clroot.ball.adapter.outbound.data.access.core.exception.DatabaseException
import io.clroot.ball.adapter.outbound.data.access.core.exception.DuplicateEntityException
import io.clroot.ball.adapter.outbound.data.access.jpa.record.AggregateRootRecord
import io.clroot.ball.domain.model.AggregateRoot
import io.clroot.ball.domain.model.vo.BinaryId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.*

class JpaRepositoryAdapterTest :
    DescribeSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        describe("JpaRepositoryAdapter") {
            beforeEach {
                clearAllMocks()
            }

            describe("UUID ID 사용 시") {
                fun createTestData() = Triple(
                    mockk<JpaRepository<TestBinaryJpaRecord, UUID>>(relaxed = true),
                    BinaryId.generate(),
                    LocalDateTime.now()
                )

                fun createTestEntity(testId: BinaryId, now: LocalDateTime) = TestBinaryEntity(
                    id = testId,
                    name = "Test Entity",
                    createdAt = now,
                    updatedAt = now,
                    version = 0,
                )

                context("findById") {
                    it("같은 ID 값을 가진 엔티티를 찾을 수 있어야 한다") {
                        val (springDataRepository, testId, now) = createTestData()
                        val adapter = TestBinaryRepositoryAdapter(springDataRepository)
                        val byteArrayId = testId.uuid
                        val jpaRecord =
                            TestBinaryJpaRecord(
                                id = byteArrayId,
                                name = "Test Entity",
                                createdAt = now,
                                updatedAt = now,
                                version = 0,
                            )

                        val capturedId = slot<UUID>()
                        every { springDataRepository.findById(capture(capturedId)) } returns Optional.of(jpaRecord)

                        val result = adapter.findById(testId)

                        result shouldNotBe null
                        result?.id shouldBe BinaryId.of(byteArrayId)
                        result?.name shouldBe "Test Entity"

                        // UUID 내용이 같은지 확인
                        capturedId.captured shouldNotBe null
                        capturedId.captured shouldBe byteArrayId
                    }

                    it("UUID ID로 조회 시 null을 반환할 수 있어야 한다") {
                        val (springDataRepository, testId, _) = createTestData()
                        val adapter = TestBinaryRepositoryAdapter(springDataRepository)
                        every { springDataRepository.findById(any<UUID>()) } returns Optional.empty()

                        val result = adapter.findById(testId)

                        result shouldBe null
                    }

                    it("예외 발생 시 DatabaseException으로 래핑되어야 한다") {
                        val (springDataRepository, testId, _) = createTestData()
                        val adapter = TestBinaryRepositoryAdapter(springDataRepository)
                        every { springDataRepository.findById(any<UUID>()) } throws RuntimeException("DB Error")

                        shouldThrow<DatabaseException> {
                            adapter.findById(testId)
                        }
                    }
                }

                context("save") {
                    it("새로운 엔티티를 저장할 때 UUID ID가 올바르게 변환되어야 한다") {
                        val (springDataRepository, testId, now) = createTestData()
                        val adapter = TestBinaryRepositoryAdapter(springDataRepository)
                        val testEntity = createTestEntity(testId, now)
                        val byteArrayId = testId.uuid
                        val jpaRecord =
                            TestBinaryJpaRecord(
                                id = byteArrayId,
                                name = "Test Entity",
                                createdAt = testEntity.createdAt,
                                updatedAt = testEntity.updatedAt,
                                version = 0,
                            )

                        every { springDataRepository.findByIdOrNull(any<UUID>()) } returns null
                        every { springDataRepository.save(any<TestBinaryJpaRecord>()) } returns jpaRecord

                        val result = adapter.save(testEntity)

                        result.id shouldBe BinaryId.of(byteArrayId)
                        result.name shouldBe "Test Entity"

                        verify {
                            springDataRepository.findByIdOrNull(match<UUID> { it == byteArrayId })
                            springDataRepository.save(
                                match<TestBinaryJpaRecord> {
                                    it.id == byteArrayId && it.name == "Test Entity"
                                },
                            )
                        }
                    }

                    it("기존 엔티티 업데이트 시 UUID ID로 정확히 찾아야 한다") {
                        val (springDataRepository, testId, now) = createTestData()
                        val adapter = TestBinaryRepositoryAdapter(springDataRepository)
                        val testEntity = createTestEntity(testId, now)
                        val byteArrayId = testId.uuid
                        val existingRecord =
                            TestBinaryJpaRecord(
                                id = byteArrayId,
                                name = "Old Name",
                                createdAt = testEntity.createdAt,
                                updatedAt = testEntity.updatedAt,
                                version = 0,
                            )

                        val updatedEntity = testEntity.copy(name = "Updated Name")

                        every { springDataRepository.findByIdOrNull(any<UUID>()) } returns existingRecord

                        val result = adapter.save(updatedEntity)

                        result.name shouldBe "Updated Name"
                        existingRecord.name shouldBe "Updated Name"

                        verify {
                            springDataRepository.findByIdOrNull(match<UUID> { it == byteArrayId })
                        }
                    }

                    it("중복 엔티티 저장 시 DuplicateEntityException이 발생해야 한다") {
                        val (springDataRepository, testId, now) = createTestData()
                        val adapter = TestBinaryRepositoryAdapter(springDataRepository)
                        val testEntity = createTestEntity(testId, now)
                        every { springDataRepository.findByIdOrNull(any<UUID>()) } returns null
                        every { springDataRepository.save(any<TestBinaryJpaRecord>()) } throws
                            DataIntegrityViolationException("Duplicate key")

                        shouldThrow<DuplicateEntityException> {
                            adapter.save(testEntity)
                        }
                    }
                }
            }

            describe("일반 ID 타입 사용 시") {
                fun createStringTestData() = Triple(
                    mockk<JpaRepository<TestStringJpaRecord, String>>(relaxed = true),
                    TestStringId("test-${UUID.randomUUID()}"),
                    LocalDateTime.now()
                )

                fun createStringTestEntity(testId: TestStringId, now: LocalDateTime) = TestStringEntity(
                    id = testId,
                    name = "Test Entity",
                    createdAt = now,
                    updatedAt = now,
                    version = 0,
                )

                context("findAll") {
                    it("모든 엔티티를 조회할 수 있어야 한다") {
                        val (springDataRepository, _, now) = createStringTestData()
                        val adapter = TestStringRepositoryAdapter(springDataRepository)
                        val jpaRecords =
                            listOf(
                                TestStringJpaRecord("1", "Entity 1", now, now, null, 0),
                                TestStringJpaRecord("2", "Entity 2", now, now, null, 0),
                            )

                        every { springDataRepository.findAll() } returns jpaRecords

                        val result = adapter.findAll()

                        result.size shouldBe 2
                        result[0].id.value shouldBe "1"
                        result[1].id.value shouldBe "2"
                    }

                    it("예외 발생 시 DatabaseException으로 래핑되어야 한다") {
                        val (springDataRepository, _, _) = createStringTestData()
                        val adapter = TestStringRepositoryAdapter(springDataRepository)
                        every { springDataRepository.findAll() } throws RuntimeException("DB Error")

                        shouldThrow<DatabaseException> {
                            adapter.findAll()
                        }
                    }
                }
            }
        }
    })

// 테스트용 도메인 모델 및 JPA 레코드 정의
class TestBinaryEntity(
    id: BinaryId,
    val name: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime? = null,
    version: Long,
) : AggregateRoot<BinaryId>(id, createdAt, updatedAt, deletedAt, version) {
    fun copy(name: String = this.name) = TestBinaryEntity(id, name, createdAt, updatedAt, deletedAt, version)
}

@Entity
class TestBinaryJpaRecord(
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
    var id: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime? = null,
    version: Long,
) : AggregateRootRecord<TestBinaryEntity>(createdAt, updatedAt, deletedAt, version) {
    override fun update(entity: TestBinaryEntity) {
        this.name = entity.name
        updateCommonFields(entity)
    }
}

// String ID를 사용하는 엔티티
@JvmInline
value class TestStringId(
    val value: String,
)

class TestStringEntity(
    id: TestStringId,
    val name: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime? = null,
    version: Long,
) : AggregateRoot<TestStringId>(id, createdAt, updatedAt, deletedAt, version)

@Entity
class TestStringJpaRecord(
    @Id
    @Column(name = "id", nullable = false)
    var id: String,
    @Column(name = "name", nullable = false)
    var name: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime? = null,
    version: Long,
) : AggregateRootRecord<TestStringEntity>(createdAt, updatedAt, deletedAt, version) {
    override fun update(entity: TestStringEntity) {
        this.name = entity.name
        updateCommonFields(entity)
    }
}

// 테스트용 어댑터 구현
class TestBinaryRepositoryAdapter(
    springDataRepository: JpaRepository<TestBinaryJpaRecord, UUID>,
) : JpaRepositoryAdapter<TestBinaryEntity, BinaryId, TestBinaryJpaRecord, UUID>(
        springDataRepository,
    ) {
    override fun BinaryId.toJpaId(): UUID = this.uuid

    override fun TestBinaryJpaRecord.toDomain(): TestBinaryEntity =
        TestBinaryEntity(
            id = BinaryId.of(this.id),
            name = this.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            deletedAt = this.deletedAt,
            version = this.version,
        )

    override fun TestBinaryEntity.toJpa(): TestBinaryJpaRecord =
        TestBinaryJpaRecord(
            id = this.id.uuid,
            name = this.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            deletedAt = this.deletedAt,
            version = this.version,
        )

    override fun delete(id: BinaryId) {
        hardDelete(id)
    }
}

class TestStringRepositoryAdapter(
    springDataRepository: JpaRepository<TestStringJpaRecord, String>,
) : JpaRepositoryAdapter<TestStringEntity, TestStringId, TestStringJpaRecord, String>(
        springDataRepository,
    ) {
    override fun TestStringId.toJpaId(): String = this.value

    override fun TestStringJpaRecord.toDomain(): TestStringEntity =
        TestStringEntity(
            id = TestStringId(this.id),
            name = this.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            deletedAt = this.deletedAt,
            version = this.version,
        )

    override fun TestStringEntity.toJpa(): TestStringJpaRecord =
        TestStringJpaRecord(
            id = this.id.value,
            name = this.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            deletedAt = this.deletedAt,
            version = this.version,
        )

    override fun delete(id: TestStringId) {
        hardDelete(id)
    }
}
