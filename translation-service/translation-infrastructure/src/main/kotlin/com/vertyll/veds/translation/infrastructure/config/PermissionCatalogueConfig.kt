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
            permission("TRANSLATIONS_VIEW", "View the translation catalogue", RoleScope.GLOBAL)
            permission("TRANSLATIONS_EDIT", "Edit translations and import them in bulk", RoleScope.GLOBAL)
        }
}
