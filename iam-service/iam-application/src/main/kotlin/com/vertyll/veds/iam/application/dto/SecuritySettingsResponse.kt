package com.vertyll.veds.iam.application.dto

/**
 * The caller's second factor: whether it is on, the credential types Keycloak holds, and the id of the one-time-password
 * credential, which Keycloak's `delete_credential` action takes to turn the second factor off.
 */
data class SecuritySettingsResponse(
    val twoFactorEnabled: Boolean,
    val configuredFactors: List<String>,
    val twoFactorCredentialId: String?,
)
