package com.vertyll.veds.sharedinfrastructure.saga.contract

/**
 * Marker interface for enums that represent saga type identifiers.
 * Any enum used as a saga type or step name must implement this interface.
 *
 * Example:
 * ```
 * enum class SagaTypes(override val value: String) : SagaTypeValue {
 *     USER_REGISTRATION("UserRegistration")
 * }
 * ```
 */
interface SagaTypeValue {
    val value: String
}
