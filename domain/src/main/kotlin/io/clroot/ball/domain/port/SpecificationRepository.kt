package io.clroot.ball.domain.port

import io.clroot.ball.domain.model.EntityBase
import io.clroot.ball.domain.model.specification.Specification

/**
 * 명세 패턴 기반 리포지토리 인터페이스
 * 
 * Repository 인터페이스를 확장하여 DDD의 Specification 패턴을 활용한 
 * 고급 쿼리 기능을 제공합니다. 복잡한 비즈니스 규칙을 캡슐화하고
 * 재사용 가능한 쿼리 조건을 정의할 수 있습니다.
 * 
 * 기본 CRUD 작업 외에 다음과 같은 고급 기능을 제공합니다:
 * - 명세를 만족하는 엔티티 조회
 * - 존재 여부 확인
 * - 조건부 카운팅
 * 
 * @param T 관리할 엔티티 타입 (EntityBase를 상속해야 함)
 * @param ID 엔티티의 식별자 타입
 * 
 * @since 2.0
 * @see Repository
 * @see Specification
 */
interface SpecificationRepository<T : EntityBase<ID>, ID : Any> : Repository<T, ID> {
    
    /**
     * 명세를 만족하는 모든 엔티티를 조회합니다.
     * 
     * Specification 패턴을 사용하여 복잡한 쿼리 조건을 
     * 재사용 가능하고 테스트 가능한 형태로 캡슐화할 수 있습니다.
     * 
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 명세를 만족하는 엔티티 목록
     * @throws PersistenceException 조회 중 오류가 발생한 경우
     * 
     * @since 2.0
     * 
     * @sample
     * ```kotlin
     * val activeUsers = userRepository.findBySpecification(ActiveUserSpecification())
     * ```
     */
    fun findBySpecification(specification: Specification<T>): List<T>

    /**
     * 명세를 만족하는 첫 번째 엔티티를 조회합니다.
     * 
     * 명세를 만족하는 엔티티가 여러 개 있어도 첫 번째 엔티티만 반환합니다.
     * 조건을 만족하는 엔티티가 없으면 null을 반환합니다.
     * 
     * 기본 구현은 findBySpecification을 호출한 후 첫 번째 요소를 추출합니다.
     * 성능 최적화가 필요한 경우 구현체에서 오버라이드할 수 있습니다.
     * 
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 명세를 만족하는 첫 번째 엔티티, 없으면 null
     * @throws PersistenceException 조회 중 오류가 발생한 경우
     * 
     * @since 2.0
     */
    fun findOneBySpecification(specification: Specification<T>): T? {
        val entities = findBySpecification(specification)
        return entities.firstOrNull()
    }

    /**
     * 주어진 식별자를 가진 엔티티가 존재하는지 확인합니다.
     * 
     * 전체 엔티티를 로드하지 않고 존재 여부만 확인하므로 
     * findById보다 성능상 유리할 수 있습니다.
     * 
     * @param id 확인할 엔티티의 식별자
     * @return 엔티티가 존재하면 true, 존재하지 않으면 false
     * @throws PersistenceException 조회 중 오류가 발생한 경우
     * 
     * @since 2.0
     */
    fun existsById(id: ID): Boolean

    /**
     * 명세를 만족하는 엔티티가 존재하는지 확인합니다.
     * 
     * 전체 엔티티를 로드하지 않고 존재 여부만 확인하므로
     * findBySpecification보다 성능상 유리할 수 있습니다.
     * 
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 명세를 만족하는 엔티티가 존재하면 true, 존재하지 않으면 false
     * @throws PersistenceException 조회 중 오류가 발생한 경우
     * 
     * @since 2.0
     * 
     * @sample
     * ```kotlin
     * val hasActiveUsers = userRepository.existsBySpecification(ActiveUserSpecification())
     * ```
     */
    fun existsBySpecification(specification: Specification<T>): Boolean

    /**
     * 명세를 만족하는 엔티티의 개수를 계산합니다.
     * 
     * 전체 엔티티를 로드하지 않고 개수만 계산하므로
     * findBySpecification 후 size를 확인하는 것보다 성능상 유리합니다.
     * 
     * @param specification 엔티티가 만족해야 하는 명세
     * @return 명세를 만족하는 엔티티 개수
     * @throws PersistenceException 조회 중 오류가 발생한 경우
     * 
     * @since 2.0
     * 
     * @sample
     * ```kotlin
     * val activeUserCount = userRepository.countBySpecification(ActiveUserSpecification())
     * ```
     */
    fun countBySpecification(specification: Specification<T>): Long
}
