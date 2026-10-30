package com.vertyll.veds.sharedauthz

/** Where a role is held: across the platform, or inside one project. */
enum class RoleScope {
    /** The role applies across the whole application. */
    GLOBAL,

    /** The role applies inside one project. */
    PROJECT,
}
