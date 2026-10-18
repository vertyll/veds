package com.vertyll.veds.template.infrastructure.web.dto

import com.vertyll.veds.template.application.command.CreateTemplateCommand
import jakarta.validation.constraints.NotBlank

data class CreateTemplateRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val payload: String,
) {
    fun toCommand(): CreateTemplateCommand =
        CreateTemplateCommand(
            name = name,
            payload = payload,
        )
}
