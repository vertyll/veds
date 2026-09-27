package com.vertyll.veds.iam.infrastructure.kafka

internal object IamKafkaTopics {
    const val USER_REGISTERED = "user-registered"
    const val USER_PROFILE_UPDATED = "user-profile-updated"
    const val ROLE_PERMISSIONS_CHANGED = "role-permissions-changed"

    const val MAIL_REQUESTED = "mail-requested"
}
