package io.clroot.ball.user.domain.port

import io.clroot.ball.shared.core.model.Entity

/**
 * 속성 영속성 제공자 (Attribute Persistence Provider)
 *
 * 엔티티의 확장 속성을 영속화하고 로드하는 메커니즘을 정의하는 인터페이스
 */
interface AttributePersistenceProvider {
    /**
     * 엔티티의 속성 로드
     *
     * @param entity 속성을 로드할 엔티티
     * @param dataModel 데이터 모델 객체
     * @return 속성이 로드된 엔티티
     */
    fun <E : Entity<*>> loadAttributes(entity: E, dataModel: Any): E

    /**
     * 엔티티의 속성 저장
     *
     * @param entity 속성을 저장할 엔티티
     * @param dataModel 데이터 모델 객체
     */
    fun <E : Entity<*>> saveAttributes(entity: E, dataModel: Any)
}