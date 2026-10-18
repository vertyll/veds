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
            permission("MAIL_LOGS_VIEW", "Read the record of what the system sent", RoleScope.GLOBAL)
            permission("MAIL_SEND", "Send mail through the service", RoleScope.GLOBAL)
        }
}
