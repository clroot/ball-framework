package io.clroot.ball.adapter.outbound.data.access.core.exception

import io.clroot.ball.domain.exception.DomainErrorCodes
import io.clroot.ball.domain.exception.ErrorType

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
 *         throw DuplicateEntityException("이미 존재하는 엔티티입니다")
 *     }
 * }
 * ```
 */
class DuplicateEntityException(
    message: String,
    metadata: Map<String, Any?> = emptyMap(),
) : PersistenceException(
        message = message,
        errorType = ErrorType.CONFLICT,
        code = DomainErrorCodes.PERSISTENCE_DUPLICATE,
        messageKey = "persistence.duplicate",
        metadata = metadata,
    )
