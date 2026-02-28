package com.vertyll.veds.template.application.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/template")
@Tag(name = "Template", description = "Template management API")
class TemplateController
