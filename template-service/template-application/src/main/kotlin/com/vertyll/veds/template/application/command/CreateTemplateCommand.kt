package com.vertyll.veds.template.application.command

data class CreateTemplateCommand(
    val name: String,
    val payload: String,
)
