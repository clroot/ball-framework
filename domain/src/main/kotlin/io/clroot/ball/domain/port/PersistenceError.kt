package io.clroot.ball.domain.port

/**
 * 영속성 계층에서 발생할 수 있는 오류를 정의한 sealed class
 * 
 * Repository 인터페이스의 작업 중 발생할 수 있는 다양한 오류 상황을 
 * 타입 안전하게 표현합니다. sealed class를 사용하여 컴파일 타임에
 * 모든 경우를 처리하도록 강제할 수 있습니다.
 * 
 * Arrow-kt의 Either와 함께 사용되어 함수형 에러 처리를 가능하게 합니다.
 * 
 * @since 2.0
 * 
 * @sample
 * ```kotlin
 * when (result) {
 *     is Either.Left -> when (result.value) {
 *         is PersistenceError.DatabaseError -> handleDatabaseError(result.value.cause)
 *         is PersistenceError.EntityNotFound -> handleNotFound()
 *         is PersistenceError.DuplicateEntity -> handleDuplicate()
 *     }
 *     is Either.Right -> handleSuccess(result.value)
 * }
 * ```
 */
sealed class PersistenceError {
    
    /**
     * 데이터베이스 수준에서 발생한 오류
     * 
     * 연결 실패, SQL 오류, 트랜잭션 오류 등 데이터베이스 시스템에서
     * 발생하는 모든 예외 상황을 나타냅니다.
     * 
     * @property cause 실제 발생한 예외 객체
     * 
     * @since 2.0
     * 
     * @sample
     * ```kotlin
     * // JPA 구현체에서 사용 예시
     * try {
     *     entityManager.persist(entity)
     * } catch (e: PersistenceException) {
     *     Either.Left(PersistenceError.DatabaseError(e))
     * }
     * ```
     */
    data class DatabaseError(val cause: Throwable) : PersistenceError()

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
     * fun findById(id: UserId): Either<PersistenceError, User> {
     *     val user = userJpaRepository.findById(id.value)
     *     return if (user.isPresent) {
     *         Either.Right(user.get().toDomainEntity())
     *     } else {
     *         Either.Left(PersistenceError.EntityNotFound)
     *     }
     * }
     * ```
     */
    data object EntityNotFound : PersistenceError()

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
     * fun save(user: User): Either<PersistenceError, User> {
     *     return try {
     *         val savedUser = userJpaRepository.save(user.toJpaEntity())
     *         Either.Right(savedUser.toDomainEntity())
     *     } catch (e: DataIntegrityViolationException) {
     *         Either.Left(PersistenceError.DuplicateEntity)
     *     }
     * }
     * ```
     */
    data object DuplicateEntity : PersistenceError()
}
