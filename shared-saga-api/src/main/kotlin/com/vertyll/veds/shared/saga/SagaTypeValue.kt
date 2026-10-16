package com.vertyll.veds.shared.saga

/**
 * Marker for an enum that names the saga types of one bounded context.
 *
 * Lives in `shared-saga-api` rather than in `shared-saga-engine` so that an
 * application layer can reference a saga type without putting Spring on its
 * compile classpath.
 */
interface SagaTypeValue {
    val value: String
}
