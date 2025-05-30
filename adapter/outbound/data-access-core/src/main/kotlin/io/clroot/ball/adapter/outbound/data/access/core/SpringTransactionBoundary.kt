package io.clroot.ball.adapter.outbound.data.access.core

import io.clroot.ball.application.port.outbound.TransactionBoundary
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Component
class SpringTransactionBoundary(
    private val platformTransactionManager: PlatformTransactionManager
) : TransactionBoundary {

    private val defaultTransactionTemplate = TransactionTemplate(platformTransactionManager)

    override fun <T> execute(block: () -> T): T {
        return defaultTransactionTemplate.execute { block() }!!
    }

    override fun <T> executeIsolated(block: () -> T): T {
        val transactionTemplate = TransactionTemplate(platformTransactionManager)
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        return transactionTemplate.execute { block() }!!
    }
}