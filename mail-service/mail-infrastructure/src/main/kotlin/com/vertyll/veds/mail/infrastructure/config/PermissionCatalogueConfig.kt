package com.vertyll.veds.mail.infrastructure.config

import com.vertyll.veds.sharedauthz.PermissionCatalogue
import com.vertyll.veds.sharedauthz.RoleScope
import com.vertyll.veds.sharedauthz.permissions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class PermissionCatalogueConfig {
    @Bean
    fun mailPermissionCatalogue(): PermissionCatalogue =
        permissions("mail") {
            permission(name = "MAIL_LOGS_VIEW", description = "Read the record of what the system sent", scope = RoleScope.GLOBAL)
            permission(name = "MAIL_SEND", description = "Send mail through the service", scope = RoleScope.GLOBAL)
        }
}
