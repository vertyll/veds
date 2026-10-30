package com.vertyll.veds.translation.infrastructure.config

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class PermissionCatalogueConfig {
    @Bean
    fun translationPermissionCatalogue(): PermissionCatalogue =
        permissions("translation") {
            permission(name = "TRANSLATIONS_VIEW", description = "View the translation catalogue", scope = RoleScope.GLOBAL)
            permission(name = "TRANSLATIONS_EDIT", description = "Edit translations and import them in bulk", scope = RoleScope.GLOBAL)
        }
}
