package io.clroot.ball.shared.attribute

import io.clroot.ball.shared.core.model.Entity

interface AttributePersistenceProvider {
    fun <E : Entity<*>> loadAttributes(entity: E, dataModel: Any): E

    fun <E : Entity<*>> saveAttributes(entity: E, dataModel: Any)
}
