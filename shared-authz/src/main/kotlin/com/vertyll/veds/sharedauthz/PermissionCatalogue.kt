package com.vertyll.veds.sharedauthz

/**
 * The permissions one module enforces, declared by the service that owns them.
 *
 * A permission is real only where some code checks it, so the catalogue is code
 * rather than data: nothing may invent a permission the owning service does not
 * know how to enforce. What *is* data is which permissions a role holds — that
 * belongs to iam-service and is edited by administrators.
 */
data class PermissionCatalogue(
    val module: String,
    val definitions: List<PermissionDefinition>,
    val stockRoles: List<StockRole> = emptyList(),
) {
    init {
        require(module.isNotBlank()) { "a permission catalogue must name its module" }
        require(definitions.isNotEmpty()) { "module '$module' declared no permissions" }

        val byName = definitions.associateBy { it.name }
        stockRoles.forEach { role ->
            val unknown = role.permissions - byName.keys
            require(unknown.isEmpty()) {
                "stock role '${role.name}' of module '$module' grants permissions this module does not declare: $unknown"
            }

            val wrongScope = role.permissions.filter { byName.getValue(it).scope != role.scope }
            require(wrongScope.isEmpty()) {
                "stock role '${role.name}' is ${role.scope} but grants permissions held in another scope: $wrongScope"
            }
        }
    }

    val names: Set<String> get() = definitions.mapTo(linkedSetOf()) { it.name }
}
