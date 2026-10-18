package com.vertyll.veds.iam.infrastructure.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

@Component
internal class TransactionalUseCaseFactory(
    transactionManager: PlatformTransactionManager,
) {
    private val readWrite = TransactionTemplate(transactionManager)

    private val readOnly = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    fun <T : Any> wrap(
        contract: Class<T>,
        target: T,
        isReadOnly: (String) -> Boolean,
    ): T {
        val proxy =
            Proxy.newProxyInstance(contract.classLoader, arrayOf(contract)) { _, method, args ->
                val template = if (isReadOnly(method.name)) readOnly else readWrite
                template.execute {
                    try {
                        method.invoke(target, *(args ?: emptyArray()))
                    } catch (e: InvocationTargetException) {
                        throw e.targetException
                    }
                }
            }

        @Suppress("UNCHECKED_CAST")
        return proxy as T
    }
}
