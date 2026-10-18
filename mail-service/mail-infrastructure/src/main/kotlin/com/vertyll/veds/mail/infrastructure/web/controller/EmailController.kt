package com.vertyll.veds.mail.infrastructure.web.controller

import com.vertyll.veds.mail.application.dto.EmailLogResponse
import com.vertyll.veds.mail.application.dto.EmailResult
import com.vertyll.veds.mail.application.dto.SendBatchEmailResponse
import com.vertyll.veds.mail.application.dto.SendEmailResponse
import com.vertyll.veds.mail.application.port.inbound.EmailBatchUseCase
import com.vertyll.veds.mail.application.port.inbound.EmailUseCase
import com.vertyll.veds.mail.domain.error.MailError
import com.vertyll.veds.mail.domain.model.EmailTemplate
import com.vertyll.veds.mail.domain.model.PageResult
import com.vertyll.veds.mail.infrastructure.web.dto.SendBatchEmailRequest
import com.vertyll.veds.mail.infrastructure.web.dto.SendEmailRequest
import com.vertyll.veds.sharederror.ApiException
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mail")
internal class EmailController(
    private val emailService: EmailUseCase,
    private val emailBatchService: EmailBatchUseCase,
) {
    @GetMapping("/logs")
    @PreAuthorize("@authz.has('MAIL_LOGS_VIEW')")
    fun getEmailLogs(): ResponseEntity<PageResult<EmailLogResponse>> {
        val logs = emailService.getEmailLogs()
        return ResponseEntity.ok(logs)
    }

    @PostMapping("/send")
    @PreAuthorize("@authz.has('MAIL_SEND')")
    fun sendEmail(
        @Valid @RequestBody
        request: SendEmailRequest,
    ): ResponseEntity<SendEmailResponse> {
        val template =
            EmailTemplate.fromTemplateName(request.templateName)
                ?: throw ApiException(MailError.TEMPLATE_UNKNOWN, mapOf("templateName" to request.templateName))

        val success =
            emailService.sendEmail(
                to = request.to,
                template = template,
                variables = request.variables,
                replyTo = request.replyTo,
            )

        return ResponseEntity.ok(
            SendEmailResponse(
                success = success,
                message = if (success) "Email successfully sent to ${request.to}" else "Failed to send email to ${request.to}",
            ),
        )
    }

    @PostMapping("/send-batch")
    @PreAuthorize("@authz.has('MAIL_SEND')")
    fun sendBatchEmail(
        @Valid @RequestBody
        request: SendBatchEmailRequest,
    ): ResponseEntity<SendBatchEmailResponse> {
        val template =
            EmailTemplate.fromTemplateName(request.templateName)
                ?: throw ApiException(MailError.TEMPLATE_UNKNOWN, mapOf("templateName" to request.templateName))

        val results =
            emailBatchService.processEmailBatch(
                recipients = request.recipients,
                template = template,
                commonVariables = request.commonVariables,
                specificVariables = request.specificVariables,
                replyTo = request.replyTo,
            )

        val successCount = results.count { it.value }
        val failureCount = results.size - successCount

        return ResponseEntity.ok(
            SendBatchEmailResponse(
                totalRecipients = results.size,
                successCount = successCount,
                failureCount = failureCount,
                details =
                    results.map { (recipient, success) ->
                        EmailResult(recipient, success)
                    },
            ),
        )
    }
}
