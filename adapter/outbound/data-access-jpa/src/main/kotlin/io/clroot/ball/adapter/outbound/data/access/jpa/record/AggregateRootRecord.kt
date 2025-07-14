package io.clroot.ball.adapter.outbound.data.access.jpa.record

import io.clroot.ball.domain.model.AggregateRoot
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import java.time.LocalDateTime

/**
 * 제네릭 기반 JPA 집합체 루트 레코드 기본 클래스
 *
 * EntityRecord를 확장하여 낙관적 잠금을 위한 버전 정보를 추가합니다.
 * ID 타입에 의존하지 않는 추상 클래스로, 사용자가 구체적인 ID 타입을 정의할 수 있습니다.
 *
 * @param E 도메인 집합체 루트 타입
 * @param ID 식별자 타입 (BinaryId, UserId, OrderId 등)
 */
@MappedSuperclass
abstract class AggregateRootRecord<E : AggregateRoot<*>>(
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    version: Long,
) : EntityRecord<E>(createdAt, updatedAt) {
    /**
     * 낙관적 잠금을 위한 버전 필드
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = version
        protected set

    /**
     * 도메인 집합체 루트로부터 생성하는 생성자
     *
     * @param entity 도메인 집합체 루트
     * @param version 초기 버전 (기본값: 0)
     */
    constructor(entity: AggregateRoot<*>, version: Long = 0L) : this(
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        version = version,
    )

    /**
     * 도메인 집합체 루트의 공통 필드를 업데이트합니다.
     * 버전은 JPA에서 자동으로 관리되므로 수동으로 업데이트하지 않습니다.
     */
    protected fun updateCommonFields(entity: AggregateRoot<*>) {
        super.updateCommonFields(entity)
        // version은 JPA @Version에 의해 자동 관리됨
    }
}
