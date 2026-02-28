package com.vertyll.veds.mail.application.controller

import com.vertyll.veds.mail.domain.dto.EmailLogResponseDto
import com.vertyll.veds.mail.domain.dto.EmailResult
import com.vertyll.veds.mail.domain.dto.SendBatchEmailRequest
import com.vertyll.veds.mail.domain.dto.SendBatchEmailResponse
import com.vertyll.veds.mail.domain.dto.SendEmailRequest
import com.vertyll.veds.mail.domain.dto.SendEmailResponse
import com.vertyll.veds.mail.domain.model.enums.EmailTemplate
import com.vertyll.veds.mail.domain.service.EmailSagaService
import com.vertyll.veds.mail.domain.service.EmailService
import com.vertyll.veds.mail.infrastructure.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mail")
class EmailController(
    private val emailSagaService: EmailSagaService,
    private val emailService: EmailService,
) {
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    fun getEmailLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<EmailLogResponseDto>>> {
        val pageable = PageRequest.of(page, size)
        val logs = emailService.getEmailLogs(pageable)
        return ApiResponse.buildResponse(
            data = logs,
            message = "Email logs retrieved successfully",
            status = HttpStatus.OK,
        )
    }

    @PostMapping("/send")
    fun sendEmail(
        @Valid @RequestBody
        request: SendEmailRequest,
    ): ResponseEntity<ApiResponse<SendEmailResponse>> {
        val template =
            EmailTemplate.fromTemplateName(request.templateName)
                ?: return ApiResponse.buildResponse(
                    data = null,
                    message = "Invalid template name: ${request.templateName}",
                    status = HttpStatus.BAD_REQUEST,
                )

        val success =
            emailSagaService.sendEmailWithSaga(
                to = request.to,
                subject = request.subject,
                template = template,
                variables = request.variables,
                replyTo = request.replyTo,
            )

        return if (success) {
            ApiResponse.buildResponse(
                data =
                    SendEmailResponse(
                        success = true,
                        message = "Email successfully sent to ${request.to}",
                    ),
                message = "Email successfully sent to ${request.to}",
                status = HttpStatus.OK,
            )
        } else {
            ApiResponse.buildResponse(
                data =
                    SendEmailResponse(
                        success = false,
                        message = "Failed to send email to ${request.to}",
                    ),
                message = "Failed to send email to ${request.to}",
                status = HttpStatus.OK,
            )
        }
    }

    @PostMapping("/send-batch")
    fun sendBatchEmail(
        @Valid @RequestBody
        request: SendBatchEmailRequest,
    ): ResponseEntity<ApiResponse<SendBatchEmailResponse>> {
        val template =
            EmailTemplate.fromTemplateName(request.templateName)
                ?: return ApiResponse.buildResponse(
                    data = null,
                    message = "Invalid template name: ${request.templateName}",
                    status = HttpStatus.BAD_REQUEST,
                )

        val results =
            emailSagaService.processEmailBatch(
                recipients = request.recipients,
                subject = request.subject,
                template = template,
                commonVariables = request.commonVariables,
                specificVariables = request.specificVariables,
                replyTo = request.replyTo,
            )

        val successCount = results.count { it.value }
        val failureCount = results.size - successCount

        return ApiResponse.buildResponse(
            data =
                SendBatchEmailResponse(
                    totalRecipients = results.size,
                    successCount = successCount,
                    failureCount = failureCount,
                    details =
                        results.map { (recipient, success) ->
                            EmailResult(recipient, success)
                        },
                ),
            message = "Batch email processing completed. Success: $successCount, Failed: $failureCount",
            status = HttpStatus.OK,
        )
    }
}
