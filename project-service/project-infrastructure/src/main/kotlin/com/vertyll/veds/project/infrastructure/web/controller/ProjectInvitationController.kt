package com.vertyll.veds.project.infrastructure.web.controller

import com.vertyll.veds.project.application.dto.ProjectInvitationResponse
import com.vertyll.veds.project.application.port.inbound.command.ProjectInvitationCommandUseCase
import com.vertyll.veds.project.application.port.inbound.query.ProjectInvitationQueryUseCase
import com.vertyll.veds.project.infrastructure.web.dto.InviteMemberRequest
import com.vertyll.veds.project.infrastructure.web.dto.RespondToInvitationRequest
import com.vertyll.veds.project.infrastructure.web.security.CurrentUser
import com.vertyll.veds.shared.web.security.AuthorizedInUseCase
import com.vertyll.veds.shared.web.security.ScopedToCaller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/projects")
@Tag(name = "Project invitations", description = "Invite users to a project and respond to invitations")
internal class ProjectInvitationController(
    private val invitationServiceCommands: ProjectInvitationCommandUseCase,
    private val invitationServiceQueries: ProjectInvitationQueryUseCase,
) {
    @PostMapping("/{projectId}/invitations")
    @AuthorizedInUseCase("ProjectInvitationCommandService — the invitation and the project decide together")
    @Operation(summary = "Invite a user to a project")
    fun invite(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable projectId: UUID,
        @Valid @RequestBody
        request: InviteMemberRequest,
    ): ResponseEntity<ProjectInvitationResponse> {
        val invitation = invitationServiceCommands.invite(projectId, request.toCommand(), CurrentUser.idOf(jwt))
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation)
    }

    @GetMapping("/invitations/me")
    @ScopedToCaller("the invitee e-mail comes from the token, so only the caller's own invitations are found")
    @Operation(summary = "List the current user's pending invitations")
    fun getMyInvitations(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<ProjectInvitationResponse>> {
        val invitations = invitationServiceQueries.getMyInvitations(CurrentUser.emailOf(jwt))
        return ResponseEntity.ok(invitations)
    }

    @PostMapping("/invitations/accept")
    @AuthorizedInUseCase("ProjectInvitationCommandService — the invitation names its invitee")
    @Operation(summary = "Accept an invitation")
    fun accept(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody
        request: RespondToInvitationRequest,
    ): ResponseEntity<ProjectInvitationResponse> {
        val invitation =
            invitationServiceCommands.acceptInvitation(
                invitationId = request.invitationId,
                actor = CurrentUser.identityOf(jwt).toActor(),
            )
        return ResponseEntity.ok(invitation)
    }

    @PostMapping("/invitations/reject")
    @AuthorizedInUseCase("ProjectInvitationCommandService — the invitation names its invitee")
    @Operation(summary = "Reject an invitation")
    fun reject(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody
        request: RespondToInvitationRequest,
    ): ResponseEntity<ProjectInvitationResponse> {
        val invitation =
            invitationServiceCommands.rejectInvitation(
                invitationId = request.invitationId,
                actor = CurrentUser.identityOf(jwt).toActor(),
            )
        return ResponseEntity.ok(invitation)
    }
}
