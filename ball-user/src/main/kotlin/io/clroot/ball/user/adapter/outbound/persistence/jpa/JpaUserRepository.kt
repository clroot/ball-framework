package io.clroot.ball.user.adapter.outbound.persistence.jpa

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import io.clroot.ball.domain.model.core.Specification
import io.clroot.ball.domain.model.vo.BinaryId
import io.clroot.ball.domain.port.PersistenceError
import io.clroot.ball.shared.attribute.AttributePersistenceProvider
import io.clroot.ball.user.domain.model.Email
import io.clroot.ball.user.domain.model.User
import io.clroot.ball.user.domain.port.UserRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * JPA 사용자 저장소 (JPA User Repository)
 *
 * JPA를 사용하여 사용자 엔티티의 영속성을 관리하는 구현체
 */
@Repository
class JpaUserRepository(
    private val userJpaRepository: UserJpaRepository,
    private val attributePersistenceProvider: AttributePersistenceProvider<User, UserRecord>
) : UserRepository {

    /**
     * 사용자 저장
     *
     * @param entity 저장할 사용자 엔티티
     * @return 성공 시 저장된 사용자 엔티티, 실패 시 오류
     */
    @Transactional
    override fun save(entity: User): Either<PersistenceError, User> {
        return try {
            val dataModel = UserRecord(entity)
            val savedModel = userJpaRepository.save(dataModel)
            attributePersistenceProvider.saveAttributes(entity, savedModel)
            toDomain(savedModel).right()
        } catch (e: Exception) {
            PersistenceError.DatabaseError(e).left()
        }
    }

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자가 존재하면 User 객체를 포함한 Option, 그렇지 않으면 None
     */
    @Transactional(readOnly = true)
    override fun findById(id: BinaryId): Option<User> {
        return userJpaRepository.findById(id)
            .map { toDomain(it) }
            .map { Option.fromNullable(it) }
            .orElse(Option.fromNullable(null))
    }

    /**
     * 모든 사용자 조회
     *
     * @return 성공 시 모든 사용자 목록, 실패 시 오류
     */
    @Transactional(readOnly = true)
    override fun findAll(): Either<PersistenceError, List<User>> {
        return try {
            val users = userJpaRepository.findAll().map { toDomain(it) }
            users.right()
        } catch (e: Exception) {
            PersistenceError.DatabaseError(e).left()
        }
    }

    /**
     * 명세를 만족하는 사용자 조회
     *
     * @param specification 사용자가 만족해야 하는 명세
     * @return 성공 시 명세를 만족하는 사용자 목록, 실패 시 오류
     */
    @Transactional(readOnly = true)
    override fun findBySpecification(specification: Specification<User>): Either<PersistenceError, List<User>> {
        // 실제 구현에서는 명세를 JPA 쿼리로 변환하는 로직이 필요
        // 여기서는 간단히 모든 사용자를 조회한 후 필터링
        return findAll().map { users ->
            users.filter { specification.isSatisfiedBy(it) }
        }
    }

    /**
     * 사용자 이름으로 사용자 조회
     *
     * @param username 사용자 이름
     * @return 사용자가 존재하면 User 객체를 포함한 Option, 그렇지 않으면 None
     */
    @Transactional(readOnly = true)
    override fun findByUsername(username: String): Option<User> {
        return userJpaRepository.findByUsername(username)
            .map { toDomain(it) }
            .map { Option.fromNullable(it) }
            .orElse(Option.fromNullable(null))
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자가 존재하면 User 객체를 포함한 Option, 그렇지 않으면 None
     */
    @Transactional(readOnly = true)
    override fun findByEmail(email: Email): Option<User> {
        return userJpaRepository.findByEmail(email.value)
            .map { toDomain(it) }
            .map { Option.fromNullable(it) }
            .orElse(Option.fromNullable(null))
    }

    /**
     * ID로 사용자 존재 여부 확인
     *
     * @param id 사용자 ID
     * @return 사용자가 존재하면 true, 그렇지 않으면 false
     */
    @Transactional(readOnly = true)
    override fun existsById(id: BinaryId): Boolean {
        return userJpaRepository.existsById(id)
    }

    /**
     * 명세를 만족하는 사용자 존재 여부 확인
     *
     * @param specification 사용자가 만족해야 하는 명세
     * @return 사용자가 존재하면 true, 그렇지 않으면 false
     */
    @Transactional(readOnly = true)
    override fun existsBySpecification(specification: Specification<User>): Boolean {
        // 실제 구현에서는 명세를 JPA 쿼리로 변환하는 로직이 필요
        // 여기서는 간단히 모든 사용자를 조회한 후 필터링
        return findAll().fold(
            { false },
            { users -> users.any { specification.isSatisfiedBy(it) } }
        )
    }

    /**
     * 사용자 이름 존재 여부 확인
     *
     * @param username 사용자 이름
     * @return 사용자 이름이 존재하면 true, 그렇지 않으면 false
     */
    @Transactional(readOnly = true)
    override fun existsByUsername(username: String): Boolean {
        return userJpaRepository.existsByUsername(username)
    }

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 이메일
     * @return 이메일이 존재하면 true, 그렇지 않으면 false
     */
    @Transactional(readOnly = true)
    override fun existsByEmail(email: Email): Boolean {
        return userJpaRepository.existsByEmail(email.value)
    }

    /**
     * 사용자 삭제
     *
     * @param entity 삭제할 사용자 엔티티
     * @return 성공 시 Unit, 실패 시 오류
     */
    @Transactional
    override fun delete(entity: User): Either<PersistenceError, Unit> {
        return try {
            userJpaRepository.deleteById(entity.id)
            Unit.right()
        } catch (e: Exception) {
            PersistenceError.DatabaseError(e).left()
        }
    }

    /**
     * ID로 사용자 삭제
     *
     * @param id 삭제할 사용자 ID
     * @return 성공 시 Unit, 실패 시 오류
     */
    @Transactional
    override fun deleteById(id: BinaryId): Either<PersistenceError, Unit> {
        return try {
            userJpaRepository.deleteById(id)
            Unit.right()
        } catch (e: Exception) {
            PersistenceError.DatabaseError(e).left()
        }
    }

    /**
     * 명세를 만족하는 사용자 수 조회
     *
     * @param specification 사용자가 만족해야 하는 명세
     * @return 성공 시 사용자 수, 실패 시 오류
     */
    @Transactional(readOnly = true)
    override fun countBySpecification(specification: Specification<User>): Either<PersistenceError, Long> {
        // 실제 구현에서는 명세를 JPA 쿼리로 변환하는 로직이 필요
        // 여기서는 간단히 모든 사용자를 조회한 후 필터링
        return findAll().map { users ->
            users.count { specification.isSatisfiedBy(it) }.toLong()
        }
    }

    /**
     * 데이터 모델을 도메인 모델로 변환
     *
     * @param dataModel 데이터 모델
     * @return 도메인 모델
     */
    private fun toDomain(dataModel: UserRecord): User {
        val user = dataModel.toDomain()
        return attributePersistenceProvider.loadAttributes(user, dataModel)
    }
}

/**
 * JPA 사용자 저장소 인터페이스 (JPA User Repository Interface)
 *
 * Spring Data JPA를 사용하여 사용자 데이터 모델에 접근하는 인터페이스
 */
@Repository
interface UserJpaRepository : JpaRepository<UserRecord, BinaryId> {
    fun findByUsername(username: String): java.util.Optional<UserRecord>
    fun findByEmail(email: String): java.util.Optional<UserRecord>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}
