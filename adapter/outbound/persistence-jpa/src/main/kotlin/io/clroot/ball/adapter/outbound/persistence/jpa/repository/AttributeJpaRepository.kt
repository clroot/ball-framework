package io.clroot.ball.adapter.outbound.persistence.jpa.repository

import io.clroot.ball.adapter.outbound.persistence.jpa.entity.AttributeEntity
import io.clroot.ball.domain.model.core.BinaryId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * 속성 JPA 리포지토리
 */
@Repository
interface AttributeJpaRepository : JpaRepository<AttributeEntity, BinaryId> {

    /**
     * 엔티티 ID와 타입으로 모든 속성 조회
     */
    fun findAllByEntityIdAndEntityType(entityId: String, entityType: String): List<AttributeEntity>

    /**
     * 엔티티 ID와 타입과 키로 속성 조회
     */
    fun findByEntityIdAndEntityTypeAndAttrKey(entityId: String, entityType: String, attrKey: String): AttributeEntity?

    /**
     * 엔티티 ID와 타입으로 모든 속성 삭제
     */
    fun deleteAllByEntityIdAndEntityType(entityId: String, entityType: String)

    /**
     * 키와 값으로 엔티티 ID 목록 조회
     */
    @Query("SELECT a.entityId FROM AttributeEntity a WHERE a.entityType = :entityType AND a.attrKey = :attrKey AND a.attrValue = :attrValue")
    fun findEntityIdsByTypeAndKeyAndValue(entityType: String, attrKey: String, attrValue: String): List<String>
}
