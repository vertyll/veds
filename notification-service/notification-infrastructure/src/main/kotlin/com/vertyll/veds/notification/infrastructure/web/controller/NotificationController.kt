package com.vertyll.veds.notification.infrastructure.web.controller

import com.vertyll.veds.notification.application.dto.NotificationResponse
import com.vertyll.veds.notification.application.dto.NotificationSettingsResponse
import com.vertyll.veds.notification.application.dto.PagedResponse
import com.vertyll.veds.notification.application.dto.UnreadCountResponse
import com.vertyll.veds.notification.application.port.inbound.command.NotificationCommandUseCase
import com.vertyll.veds.notification.application.port.inbound.query.NotificationQueryUseCase
import com.vertyll.veds.notification.domain.model.NotificationType
import com.vertyll.veds.notification.infrastructure.web.dto.DismissNotificationsRequest
import com.vertyll.veds.notification.infrastructure.web.dto.MarkReadRequest
import com.vertyll.veds.notification.infrastructure.web.dto.UpdateSettingsRequest
import com.vertyll.veds.notification.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.http.ETagUtils
import com.vertyll.veds.shared.web.security.ScopedToCaller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification inbox and delivery settings")
@ScopedToCaller("CurrentUser.idOf narrows every read and write to the recipient in the token")
internal class NotificationController(
    private val commands: NotificationCommandUseCase,
    private val queries: NotificationQueryUseCase,
) {
    private companion object {
        private const val DEFAULT_PAGE_SIZE = "20"
    }

    @GetMapping
    @Operation(summary = "List the caller's notifications")
    @Suppress("LongParameterList")
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "false") onlyUnread: Boolean,
        @RequestParam(required = false) projectId: UUID?,
        @RequestParam(required = false) type: NotificationType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) size: Int,
    ): ResponseEntity<PagedResponse<NotificationResponse>> {
        val notifications =
            queries.list(
                actorId = CurrentUser.idOf(jwt),
                onlyUnread = onlyUnread,
                projectId = projectId,
                type = type,
                page = page,
                size = size,
            )
        return ResponseEntity.ok(notifications)
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count the caller's unread notifications")
    fun unreadCount(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<UnreadCountResponse> {
        val unread = queries.unreadCount(CurrentUser.idOf(jwt))
        return ResponseEntity.ok(UnreadCountResponse(unread))
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark notifications as read")
    fun markRead(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody
        request: MarkReadRequest,
    ): ResponseEntity<Int> {
        val changed = commands.markRead(request.toCommand(), CurrentUser.idOf(jwt))
        return ResponseEntity.ok(changed)
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark every notification as read")
    fun markAllRead(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Int> {
        val changed = commands.markAllRead(CurrentUser.idOf(jwt))
        return ResponseEntity.ok(changed)
    }

    @PostMapping("/dismiss")
    @Operation(summary = "Dismiss notifications so they leave the list")
    fun dismiss(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody
        request: DismissNotificationsRequest,
    ): ResponseEntity<Int> {
        val dismissed = commands.dismiss(request.toCommand(), CurrentUser.idOf(jwt))
        return ResponseEntity.ok(dismissed)
    }

    @PostMapping("/dismiss-all")
    @Operation(summary = "Dismiss every notification")
    fun dismissAll(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Int> {
        val dismissed = commands.dismissAll(CurrentUser.idOf(jwt))
        return ResponseEntity.ok(dismissed)
    }

    @GetMapping("/settings")
    @Operation(summary = "Get the caller's delivery settings")
    fun getSettings(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<NotificationSettingsResponse> {
        val settings = queries.getSettings(CurrentUser.idOf(jwt))
        return ResponseEntity.ok(settings)
    }

    @PutMapping("/settings")
    @Operation(summary = "Update the caller's delivery settings")
    fun updateSettings(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody
        request: UpdateSettingsRequest,
        @RequestHeader(HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<NotificationSettingsResponse> {
        val settings =
            commands.updateSettings(
                command = request.toCommand(),
                actorId = CurrentUser.idOf(jwt),
                version = ETagUtils.parseIfMatchToVersion(ifMatch),
            )
        return ResponseEntity.ok(settings)
    }
}
