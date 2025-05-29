package io.clroot.ball.user.domain.port

import arrow.core.Option
import io.clroot.ball.domain.model.vo.BinaryId
import io.clroot.ball.domain.port.SpecificationRepository
import io.clroot.ball.user.domain.model.Email
import io.clroot.ball.user.domain.model.User

/**
 * 사용자 저장소 (User Repository)
 *
 * 사용자 엔티티의 영속성을 관리하는 인터페이스
 */
interface UserRepository : SpecificationRepository<User, BinaryId> {
    /**
     * 사용자 이름으로 사용자 조회
     *
     * @param username 사용자 이름
     * @return 사용자가 존재하면 User 객체를 포함한 Option, 그렇지 않으면 None
     */
    fun findByUsername(username: String): Option<User>

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자가 존재하면 User 객체를 포함한 Option, 그렇지 않으면 None
     */
    fun findByEmail(email: Email): Option<User>

    /**
     * 사용자 이름 존재 여부 확인
     *
     * @param username 사용자 이름
     * @return 사용자 이름이 존재하면 true, 그렇지 않으면 false
     */
    fun existsByUsername(username: String): Boolean

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 이메일
     * @return 이메일이 존재하면 true, 그렇지 않으면 false
     */
    fun existsByEmail(email: Email): Boolean
}