package io.clroot.ball.domain.exception

/**
 * 영속성 계층에서 발생하는 기본 예외
 * 
 * Repository 인터페이스의 작업 중 발생할 수 있는 모든 영속성 관련 예외의 기본 클래스입니다.
 * 
 * @since 2.0
 */
abstract class PersistenceException(message: String, cause: Throwable? = null) : DomainException(message, cause)

/**
 * 데이터베이스 수준에서 발생한 오류
 * 
 * 연결 실패, SQL 오류, 트랜잭션 오류 등 데이터베이스 시스템에서
 * 발생하는 모든 예외 상황을 나타냅니다.
 * 
 * @since 2.0
 * 
 * @sample
 * ```kotlin
 * // JPA 구현체에서 사용 예시
 * try {
 *     entityManager.persist(entity)
 * } catch (e: SQLException) {
 *     throw DatabaseException("Database error occurred", e)
 * }
 * ```
 */
class DatabaseException(message: String, cause: Throwable? = null) : PersistenceException(message, cause)

/**
 * 요청한 엔티티를 찾을 수 없는 경우
 * 
 * 주어진 식별자나 조건에 해당하는 엔티티가 존재하지 않을 때
 * 발생합니다. 삭제된 엔티티나 잘못된 식별자로 조회할 때 사용됩니다.
 * 
 * @since 2.0
 * 
 * @sample
 * ```kotlin
 * fun findById(id: UserId): User? {
 *     val user = userJpaRepository.findById(id.value)
 *     return if (user.isPresent) {
 *         user.get().toDomainEntity()
 *     } else {
 *         null // 또는 EntityNotFoundException을 던짐
 *     }
 * }
 * ```
 */
class EntityNotFoundException(message: String) : PersistenceException(message)

/**
 * 중복된 엔티티가 존재하는 경우
 * 
 * 유니크 제약 조건 위반이나 이미 존재하는 엔티티를 중복 생성하려 할 때
 * 발생합니다. 이메일, 사용자명 등 고유해야 하는 값의 중복 시 사용됩니다.
 * 
 * @since 2.0
 * 
 * @sample
 * ```kotlin
 * fun save(user: User): User {
 *     return try {
 *         val savedUser = userJpaRepository.save(user.toJpaEntity())
 *         savedUser.toDomainEntity()
 *     } catch (e: DataIntegrityViolationException) {
 *         throw DuplicateEntityException("Entity already exists")
 *     }
 * }
 * ```
 */
class DuplicateEntityException(message: String) : PersistenceException(message)
