package io.clroot.ball.domain.port

import io.clroot.ball.domain.exception.DomainStateException
import io.clroot.ball.domain.model.AggregateRoot
import io.clroot.ball.domain.model.EntityBase

/**
 * 기본 리포지토리 인터페이스
 *
 * 도메인 엔티티의 기본적인 CRUD(Create, Read, Update, Delete) 작업을 정의합니다.
 * 헥사고날 아키텍처에서 아웃바운드 포트 역할을 하며, 도메인 계층이 영속성 계층에
 * 의존하지 않도록 추상화를 제공합니다.
 *
 * 간소화된 에러 처리를 위해 nullable 타입과 예외를 사용합니다.
 * 영속성 관련 에러는 DomainStateException으로 처리됩니다.
 *
 * @param T 관리할 엔티티 타입 (EntityBase 또는 AggregateRoot를 상속해야 함)
 * @param ID 엔티티의 식별자 타입
 *
 * @since 2.0
 * @see SpecificationRepository
 * @see EntityBase
 * @see AggregateRoot
 */
interface Repository<T : EntityBase<ID>, ID : Any> {
    /**
     * 식별자로 엔티티를 조회합니다.
     *
     * @param id 조회할 엔티티의 식별자
     * @return 엔티티가 존재하면 엔티티 객체, 존재하지 않으면 null
     * @throws DomainStateException 조회 중 오류가 발생한 경우
     *
     * @since 2.0
     */
    fun findById(id: ID): T?

    /**
     * 모든 엔티티를 조회합니다.
     *
     * 대용량 데이터 환경에서는 성능 이슈가 발생할 수 있으므로,
     * 페이징이 필요한 경우 SpecificationRepository의 사용을 권장합니다.
     *
     * @return 엔티티 목록
     * @throws DomainStateException 조회 중 오류가 발생한 경우
     *
     * @since 2.0
     * @see SpecificationRepository
     */
    fun findAll(): List<T>

    /**
     * 엔티티를 저장하거나 업데이트합니다.
     *
     * 신규 엔티티인 경우 생성하고, 기존 엔티티인 경우 업데이트합니다.
     * 구체적인 동작은 구현체에 따라 달라질 수 있습니다.
     *
     * @param entity 저장할 엔티티
     * @return 저장된 엔티티
     * @throws DomainStateException 저장 중 오류가 발생한 경우
     *
     * @since 2.0
     */
    fun save(entity: T): T

    /**
     * 식별자로 엔티티를 조회하여 수정합니다.
     *
     * 지정된 식별자의 엔티티를 조회한 후, modifier 함수를 적용하여 엔티티를 수정합니다.
     * 이 방법은 최신 상태의 엔티티를 보장하며, 동시성 이슈를 방지하는 데 도움이 됩니다.
     *
     * 수정 작업은 트랜잭션 내에서 수행되어야 하며, 엔티티가 존재하지 않는 경우
     * DomainStateException을 발생시킵니다.
     *
     * @param id 수정할 엔티티의 식별자
     * @param modifier 엔티티를 수정하는 함수 (부수효과를 허용하는 mutable 수정)
     * @return 수정된 엔티티
     * @throws DomainStateException 엔티티가 존재하지 않는 경우
     * @throws DomainStateException 수정 중 오류가 발생한 경우
     *
     * @since 2.0
     */
    fun update(
        id: ID,
        modifier: (T) -> Unit,
    ): T

    /**
     * 기존 엔티티를 수정합니다.
     *
     * 주어진 엔티티의 식별자를 사용하여 데이터베이스에서 최신 상태를 조회한 후,
     * modifier 함수를 적용하여 수정합니다. 이는 낙관적 잠금(Optimistic Locking)이나
     * 버전 관리가 필요한 시나리오에서 유용합니다.
     *
     * 내부적으로 `update(entity.id, modifier)`를 호출하는 편의 메서드입니다.
     *
     * @param entity 수정할 엔티티 (식별자 추출 목적)
     * @param modifier 엔티티를 수정하는 함수 (부수효과를 허용하는 mutable 수정)
     * @return 수정된 엔티티
     * @throws DomainStateException 엔티티가 존재하지 않는 경우
     * @throws DomainStateException 수정 중 오류가 발생한 경우
     *
     * @see update(ID, (T) -> Unit)
     *
     * @since 2.0
     */
    fun update(
        entity: T,
        modifier: (T) -> Unit,
    ): T = update(entity.id, modifier)

    /**
     * 엔티티를 삭제합니다.
     *
     * 구현체에 따라 물리적 삭제 또는 논리적 삭제(soft delete)로 동작할 수 있습니다.
     *
     * @param entity 삭제할 엔티티
     * @throws DomainStateException 삭제 중 오류가 발생한 경우
     *
     * @since 2.0
     */
    fun delete(entity: T) {
        delete(entity.id)
    }

    /**
     * 식별자로 엔티티를 삭제합니다.
     *
     * 구현체에 따라 물리적 삭제 또는 논리적 삭제(soft delete)로 동작할 수 있습니다.
     *
     * @param id 삭제할 엔티티의 식별자
     * @throws DomainStateException 삭제 중 오류가 발생한 경우
     *
     * @since 2.0
     */
    fun delete(id: ID)
}
