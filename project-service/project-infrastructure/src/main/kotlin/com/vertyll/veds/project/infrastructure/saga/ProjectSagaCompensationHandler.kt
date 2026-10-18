package com.vertyll.veds.project.infrastructure.saga

import com.vertyll.veds.project.application.port.inbound.ProjectCompensationUseCase
import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand
import com.vertyll.veds.shared.saga.engine.CompensationCommandHandler

internal class ProjectSagaCompensationHandler(
    private val projectCompensationService: ProjectCompensationUseCase,
) : CompensationCommandHandler<ProjectCompensationCommand> {
    override fun handle(
        sagaId: String,
        command: ProjectCompensationCommand,
    ) = projectCompensationService.compensate(command)
}
