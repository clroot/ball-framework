package io.clroot.ball.domain.port

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import arrow.core.none
import io.clroot.ball.domain.model.core.EntityBase
import io.clroot.ball.domain.model.core.Specification

/**
 * 리포지토리 기본 인터페이스
 *
 * 도메인 중심 리포지토리 인터페이스로, 기본적인 CRUD 작업과
 * 명세 패턴을 활용한 쿼리 기능을 제공
 *
 * @param T 엔티티 타입
 * @param ID 엔티티 ID 타입
 */
interface RepositoryBase<T : EntityBase<ID>, ID : Any> : Repository<T, ID> {
    /**
     * 모든 엔티티 조회
     *
     * @return 성공 시 엔티티 목록, 실패 시 오류
     */
    fun findAll(): Either<PersistenceError, List<T>>
    
    /**
     * 명세를 만족하는 엔티티 조회
     *
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 성공 시 명세를 만족하는 엔티티 목록, 실패 시 오류
     */
    fun findBySpecification(specification: Specification<T>): Either<PersistenceError, List<T>>
    
    /**
     * 명세를 만족하는 단일 엔티티 조회
     *
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 성공 시 명세를 만족하는 첫 번째 엔티티, 없으면 None, 실패 시 오류
     */
    fun findOneBySpecification(specification: Specification<T>): Either<PersistenceError, Option<T>> {
        return findBySpecification(specification).map { entities ->
            if (entities.isEmpty()) none() else Some(entities.first())
        }
    }
    
    /**
     * 엔티티 존재 여부 확인
     *
     * @param id 확인할 엔티티 ID
     * @return 존재하면 true, 아니면 false
     */
    fun existsById(id: ID): Boolean
    
    /**
     * 명세를 만족하는 엔티티 존재 여부 확인
     *
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 존재하면 true, 아니면 false
     */
    fun existsBySpecification(specification: Specification<T>): Boolean
    
    /**
     * 엔티티 삭제
     *
     * @param entity 삭제할 엔티티
     * @return 성공 시 Unit, 실패 시 오류
     */
    fun delete(entity: T): Either<PersistenceError, Unit>
    
    /**
     * ID로 엔티티 삭제
     *
     * @param id 삭제할 엔티티 ID
     * @return 성공 시 Unit, 실패 시 오류
     */
    fun deleteById(id: ID): Either<PersistenceError, Unit>
    
    /**
     * 명세를 만족하는 엔티티 수 조회
     *
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 성공 시 엔티티 수, 실패 시 오류
     */
    fun countBySpecification(specification: Specification<T>): Either<PersistenceError, Long>
}