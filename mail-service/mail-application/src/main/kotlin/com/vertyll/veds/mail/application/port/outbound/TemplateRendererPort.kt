package com.vertyll.veds.mail.application.port.outbound

@Suppress("kotlin:S6517")
interface TemplateRendererPort {
    fun render(
        templateName: String,
        variables: Map<String, String>,
    ): String
}
