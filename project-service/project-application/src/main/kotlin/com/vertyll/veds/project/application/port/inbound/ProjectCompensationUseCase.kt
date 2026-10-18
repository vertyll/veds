package com.vertyll.veds.project.application.port.inbound

import com.vertyll.veds.project.application.saga.model.ProjectCompensationCommand

fun interface ProjectCompensationUseCase {
    fun compensate(command: ProjectCompensationCommand)
}
