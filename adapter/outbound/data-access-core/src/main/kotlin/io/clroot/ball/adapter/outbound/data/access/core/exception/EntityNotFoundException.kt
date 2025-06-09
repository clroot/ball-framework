package io.clroot.ball.adapter.outbound.data.access.core.exception

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