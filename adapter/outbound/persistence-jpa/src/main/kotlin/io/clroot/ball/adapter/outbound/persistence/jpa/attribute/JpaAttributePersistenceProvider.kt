package io.clroot.ball.adapter.outbound.persistence.jpa.attribute

import io.clroot.ball.adapter.outbound.persistence.jpa.entity.AttributeEntity
import io.clroot.ball.adapter.outbound.persistence.jpa.repository.AttributeJpaRepository
import io.clroot.ball.domain.model.core.BinaryId
import io.clroot.ball.shared.attribute.Attributable
import io.clroot.ball.shared.attribute.AttributeKey
import io.clroot.ball.shared.attribute.AttributePersistenceProvider
import io.clroot.ball.shared.attribute.AttributeSerializer
import io.clroot.ball.shared.core.model.Entity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * JPA 기반 속성 영속성 제공자
 * 
 * 엔티티의 속성을 JPA를 통해 데이터베이스에 저장하고 로드하는 기능을 제공합니다.
 */
@Service("jpaAttributePersistenceProvider")
class JpaAttributePersistenceProvider(
    private val attributeRepository: AttributeJpaRepository,
    private val attributeSerializer: AttributeSerializer
) : AttributePersistenceProvider {

    /**
     * 엔티티의 속성을 데이터베이스에서 로드합니다.
     * 
     * @param entity 속성을 로드할 엔티티
     * @param dataModel 데이터 모델 (현재 사용되지 않음)
     * @return 속성이 로드된 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRED)
    override fun <E : Entity<*>> loadAttributes(entity: E, dataModel: Any): E {
        // 엔티티가 Attributable이 아니면 그대로 반환
        if (entity !is Attributable<*>) {
            return entity
        }

        // 엔티티 ID와 타입 추출
        val entityId = entity.id.toString()
        val entityType = entity.javaClass.simpleName

        // 데이터베이스에서 속성 조회
        val attributeEntities = attributeRepository.findAllByEntityIdAndEntityType(entityId, entityType)
        if (attributeEntities.isEmpty()) {
            return entity
        }

        // 속성을 엔티티에 설정
        @Suppress("UNCHECKED_CAST")
        var result = entity as Attributable<E>

        for (attributeEntity in attributeEntities) {
            // 속성 값 역직렬화
            val value = attributeSerializer.deserialize<Any>(attributeEntity.attrValue, attributeEntity.valueType)
            if (value != null) {
                val key = AttributeKey(attributeEntity.attrKey, Any::class)
                @Suppress("UNCHECKED_CAST")
                result = result.setAttribute(key, value) as Attributable<E>
            }
        }

        @Suppress("UNCHECKED_CAST")
        return result as E
    }

    /**
     * 엔티티의 속성을 데이터베이스에 저장합니다.
     * 
     * @param entity 속성을 저장할 엔티티
     * @param dataModel 데이터 모델 (현재 사용되지 않음)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    override fun <E : Entity<*>> saveAttributes(entity: E, dataModel: Any) {
        // 엔티티가 Attributable이 아니면 무시
        if (entity !is Attributable<*>) {
            return
        }

        // 엔티티 ID와 타입 추출
        val entityId = entity.id.toString()
        val entityType = entity.javaClass.simpleName

        // 기존 속성 삭제
        attributeRepository.deleteAllByEntityIdAndEntityType(entityId, entityType)

        // 엔티티의 속성 추출
        val attributes = entity.attributes.getAttributes()
        if (attributes.isEmpty()) {
            return
        }

        // 속성 엔티티 생성 및 저장
        val attributeEntities = attributes.map { (key, value) ->
            val serializedValue = attributeSerializer.serialize(value)
            val valueType = value.javaClass.name

            AttributeEntity(
                id = BinaryId.new(),
                entityId = entityId,
                entityType = entityType,
                attrKey = key.name,
                attrValue = serializedValue,
                valueType = valueType
            )
        }

        attributeRepository.saveAll(attributeEntities)
    }
}
