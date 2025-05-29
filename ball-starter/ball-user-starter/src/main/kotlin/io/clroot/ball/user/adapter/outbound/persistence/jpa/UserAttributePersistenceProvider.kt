package io.clroot.ball.user.adapter.outbound.persistence.jpa

import io.clroot.ball.shared.attribute.AttributePersistenceProvider
import io.clroot.ball.user.domain.model.User
import org.springframework.stereotype.Component

@Component
class UserAttributePersistenceProvider: AttributePersistenceProvider<User, UserRecord> {
    override fun loadAttributes(entity: User, dataModel: UserRecord): User {
        TODO("Not yet implemented")
    }

    override fun saveAttributes(entity: User, dataModel: UserRecord) {
        TODO("Not yet implemented")
    }
}