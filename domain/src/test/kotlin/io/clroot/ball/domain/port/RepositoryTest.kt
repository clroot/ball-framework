package io.clroot.ball.domain.port

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import arrow.core.none
import io.clroot.ball.domain.model.core.EntityBase
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class RepositoryTest : FunSpec({
    // Create a concrete implementation of EntityBase for testing
    class TestEntity(id: String) : EntityBase<String>(id)

    // Create a mock repository
    val repository = mockk<Repository<TestEntity, String>>()

    test("findById() should return Some(entity) when entity exists") {
        val entity = TestEntity("1")
        every { repository.findById("1") } returns Some(entity)

        val result = repository.findById("1")

        result shouldBe Some(entity)
    }

    test("findById() should return None when entity does not exist") {
        every { repository.findById("2") } returns none()

        val result = repository.findById("2")

        result shouldBe none()
    }

    test("save() should return Right(entity) when save is successful") {
        val entity = TestEntity("1")
        every { repository.save(entity) } returns Either.Right(entity)

        val result = repository.save(entity)

        result.shouldBeRight(entity)
    }

    test("save() should return Left(PersistenceError) when save fails") {
        val entity = TestEntity("1")
        val error = PersistenceError.DatabaseError(RuntimeException("Database error"))
        every { repository.save(entity) } returns Either.Left(error)

        val result = repository.save(entity)

        result.shouldBeLeft(error)
    }

    test("save() should return Left(EntityNotFound) when entity is not found") {
        val entity = TestEntity("1")
        every { repository.save(entity) } returns Either.Left(PersistenceError.EntityNotFound)

        val result = repository.save(entity)

        result.shouldBeLeft(PersistenceError.EntityNotFound)
    }

    test("save() should return Left(DuplicateEntity) when entity already exists") {
        val entity = TestEntity("1")
        every { repository.save(entity) } returns Either.Left(PersistenceError.DuplicateEntity)

        val result = repository.save(entity)

        result.shouldBeLeft(PersistenceError.DuplicateEntity)
    }
})
