package io.clroot.ball.adapter.outbound.data.access.jpa

import io.clroot.ball.adapter.outbound.data.access.core.exception.DatabaseException
import io.clroot.ball.adapter.outbound.data.access.core.exception.DuplicateEntityException
import io.clroot.ball.adapter.outbound.data.access.core.exception.EntityNotFoundException
import io.clroot.ball.adapter.outbound.data.access.jpa.record.EntityRecord
import io.clroot.ball.domain.model.EntityBase
import io.clroot.ball.domain.port.Repository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository

/**
 * JPA Repository 어댑터 기본 클래스
 *
 * 반복되는 변환 로직을 줄여주는 목적
 * 복잡한 추상화 없이 단순한 보일러플레이트 제거
 */
abstract class JpaRepositoryAdapter<T : EntityBase<ID>, ID : Any, J : EntityRecord<T, ID>>(
    private val jpaRepository: JpaRepository<J, ID>,
) : Repository<T, ID> {
    protected abstract fun J.toDomain(): T

    protected abstract fun T.toJpa(): J

    fun convertJpaToDomain(jpa: J): T = jpa.toDomain()

    fun convertDomainToJpa(domain: T): J = domain.toJpa()

    override fun findById(id: ID): T? =
        try {
            jpaRepository
                .findById(id)
                .map { it.toDomain() }
                .orElse(null)
        } catch (e: Exception) {
            throw DatabaseException("Failed to find entity: $id", e)
        }

    override fun findAll(): List<T> =
        try {
            jpaRepository.findAll().map { it.toDomain() }
        } catch (e: Exception) {
            throw DatabaseException("Failed to find all entities", e)
        }

    override fun save(entity: T): T =
        try {
            val jpaEntity = entity.toJpa()
            val saved = jpaRepository.save(jpaEntity)
            saved.toDomain()
        } catch (e: DataIntegrityViolationException) {
            throw DuplicateEntityException("Entity already exists: ${entity.id}")
        } catch (e: Exception) {
            throw DatabaseException("Failed to save entity: ${entity.id}", e)
        }

    override fun update(
        id: ID,
        modifier: (T) -> Unit,
    ): T {
        val entity = findById(id) ?: throw EntityNotFoundException("Entity not found: $id")
        modifier(entity)
        return save(entity)
    }

    override fun delete(id: ID) {
        try {
            jpaRepository.deleteById(id)
        } catch (e: Exception) {
            throw DatabaseException("Failed to delete entity: $id", e)
        }
    }
}
