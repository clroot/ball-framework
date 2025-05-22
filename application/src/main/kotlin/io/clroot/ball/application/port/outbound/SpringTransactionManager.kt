package io.clroot.ball.application.port.outbound

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Component
class SpringTransactionManager(
    private val platformTransactionManager: PlatformTransactionManager
) : TransactionManager {

    private val defaultTransactionTemplate = TransactionTemplate(platformTransactionManager)

    override fun <T> withTransaction(block: () -> T): T {
        return defaultTransactionTemplate.execute { block() }!!
    }

    override fun <T> withNewTransaction(block: () -> T): T {
        val transactionTemplate = TransactionTemplate(platformTransactionManager)
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        return transactionTemplate.execute { block() }!!
    }
}