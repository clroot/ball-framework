package io.clroot.ball.adapter.outbound.data.access.jpa.record

import io.clroot.ball.adapter.outbound.data.access.core.mapping.DataModel
import io.clroot.ball.domain.model.EntityBase
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/**
 * 제네릭 기반 JPA 엔티티 레코드 기본 클래스
 *
 * ID 타입에 의존하지 않는 추상 클래스로, 사용자가 구체적인 ID 타입과 컨버터를 정의할 수 있습니다.
 *
 * @param E 도메인 엔티티 타입
 * @param ID 식별자 타입 (BinaryId, UserId, OrderId 등)
 */
@MappedSuperclass
abstract class EntityRecord<E : EntityBase<*>>(
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
) : DataModel<E> {
    @CreationTimestamp
    var createdAt: Instant = createdAt
        protected set

    @UpdateTimestamp
    var updatedAt: Instant = updatedAt
        protected set

    var deletedAt: Instant? = deletedAt
        protected set

    /**
     * 도메인 엔티티로부터 생성하는 생성자
     */
    constructor(entity: EntityBase<*>) : this(
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
    )

    /**
     * 도메인 엔티티의 공통 필드를 업데이트합니다.
     * ID는 변경되지 않으므로 업데이트에서 제외됩니다.
     */
    protected fun updateCommonFields(entity: EntityBase<*>) {
        // createdAt은 변경하지 않음
        this.updatedAt = entity.updatedAt
        this.deletedAt = entity.deletedAt
    }
}
