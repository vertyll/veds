package com.vertyll.veds.sharedinfrastructure.config

import com.fasterxml.jackson.databind.module.SimpleModule
import com.vertyll.veds.sharedinfrastructure.event.DomainEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.util.ClassUtils

@Configuration
class JacksonEventConfig {
    @Bean
    fun domainEventModule(): SimpleModule {
        val module = SimpleModule("DomainEventModule")

        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(DomainEvent::class.java))

        @Suppress("kotlin:S6524")
        val candidates = scanner.findCandidateComponents("com.vertyll.veds")
        for (candidate in candidates) {
            val clazz = ClassUtils.forName(candidate.beanClassName!!, ClassUtils.getDefaultClassLoader())
            if (DomainEvent::class.java.isAssignableFrom(clazz) && !clazz.isInterface) {
                module.registerSubtypes(clazz)
            }
        }

        return module
    }
}
