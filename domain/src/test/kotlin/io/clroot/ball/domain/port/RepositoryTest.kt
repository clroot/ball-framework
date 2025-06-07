package io.clroot.ball.domain.port

import io.clroot.ball.domain.exception.DatabaseException
import io.clroot.ball.domain.exception.DuplicateEntityException
import io.clroot.ball.domain.exception.EntityNotFoundException
import io.clroot.ball.domain.model.core.EntityBase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class RepositoryTest : FunSpec({
    // Create a concrete implementation of EntityBase for testing
    class TestEntity(id: String) : EntityBase<String>(id, Instant.now(), Instant.now(), null)

    // Create a mock repository
    val repository = mockk<Repository<TestEntity, String>>()

    context("findById operations") {
        test("should return entity when entity exists") {
            val entity = TestEntity("1")
            every { repository.findById("1") } returns entity

            val result = repository.findById("1")

            result shouldBe entity
        }

        test("should return null when entity does not exist") {
            every { repository.findById("2") } returns null

            val result = repository.findById("2")

            result shouldBe null
        }
    }

    context("save operations") {
        test("should return saved entity when save is successful") {
            val entity = TestEntity("1")
            every { repository.save(entity) } returns entity

            val result = repository.save(entity)

            result shouldBe entity
        }

        test("should throw DatabaseException when database error occurs") {
            val entity = TestEntity("1")
            every { repository.save(entity) } throws DatabaseException("Database error")

            shouldThrow<DatabaseException> {
                repository.save(entity)
            }
        }

        test("should throw DuplicateEntityException when entity already exists") {
            val entity = TestEntity("1")
            every { repository.save(entity) } throws DuplicateEntityException("Entity already exists")

            shouldThrow<DuplicateEntityException> {
                repository.save(entity)
            }
        }
    }

    context("findAll operations") {
        test("should return all entities") {
            val entities = listOf(TestEntity("1"), TestEntity("2"))
            every { repository.findAll() } returns entities

            val result = repository.findAll()

            result shouldBe entities
        }

        test("should return empty list when no entities exist") {
            every { repository.findAll() } returns emptyList()

            val result = repository.findAll()

            result shouldBe emptyList()
        }

        test("should throw DatabaseException when database error occurs") {
            every { repository.findAll() } throws DatabaseException("Database error")

            shouldThrow<DatabaseException> {
                repository.findAll()
            }
        }
    }

    context("delete operations") {
        test("should delete entity successfully") {
            val entity = TestEntity("1")
            every { repository.delete(entity) } returns Unit

            repository.delete(entity)
            // No exception should be thrown
        }

        test("should throw EntityNotFoundException when entity does not exist") {
            every { repository.delete("999") } throws EntityNotFoundException("Entity not found")

            shouldThrow<EntityNotFoundException> {
                repository.delete("999")
            }
        }
    }
})
