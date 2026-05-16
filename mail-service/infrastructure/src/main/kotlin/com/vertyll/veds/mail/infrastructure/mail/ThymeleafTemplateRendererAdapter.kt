package com.vertyll.veds.mail.infrastructure.mail

import com.vertyll.veds.mail.application.port.outbound.TemplateRendererPort
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

/**
 * Thymeleaf-backed adapter for the application-level [TemplateRendererPort].
 *
 * Copies the supplied variable map into a Thymeleaf `Context` and renders
 * the named template to a string.
 */
@Component
internal class ThymeleafTemplateRendererAdapter(
    private val templateEngine: TemplateEngine,
) : TemplateRendererPort {
    override fun render(
        templateName: String,
        variables: Map<String, String>,
    ): String {
        val context = Context()
        variables.forEach { (key, value) -> context.setVariable(key, value) }
        return templateEngine.process(templateName, context)
    }
}
