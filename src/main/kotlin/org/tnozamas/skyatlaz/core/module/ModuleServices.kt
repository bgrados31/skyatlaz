package org.tnozamas.skyatlaz.core.module

import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

/**
 * The shared systems a [ModuleContext] registers into. One holder, so adding a system (HUD, config) touches this
 * class and [ModuleContext] only, not every constructor in between.
 */
class ModuleServices(
    val events: EventBus,
    val scheduler: Scheduler,
    val commands: CommandRegistry,
)
