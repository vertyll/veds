package com.vertyll.veds.mail.application.port.out

/**
 * Outbound port for rendering a template with variable substitution.
 *
 * Implemented in the infrastructure layer by an adapter wrapping a concrete
 * template engine (e.g. Thymeleaf). Keeps the application layer free of
 * any specific templating dependency.
 */
interface TemplateRendererPort {
    /**
     * Renders the named template with the supplied variables.
     *
     * @param templateName logical template identifier (file name without extension)
     * @param variables map of variable names to string values exposed to the template
     * @return fully rendered output (typically HTML)
     */
    fun render(
        templateName: String,
        variables: Map<String, String>,
    ): String
}
