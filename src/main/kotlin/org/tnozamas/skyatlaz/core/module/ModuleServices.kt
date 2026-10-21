package org.tnozamas.skyatlaz.core.module

import org.tnozamas.skyatlaz.api.repo.NeuRepository
import org.tnozamas.skyatlaz.api.repo.PriceRepository
import org.tnozamas.skyatlaz.api.service.ServiceRepository
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.config.ConfigManager
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.keybind.KeyBindManager
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.ui.hud.HudManager

/**
 * The shared systems a [ModuleContext] registers into. One holder, so adding a system (HUD, config) touches this
 * class and [ModuleContext] only, not every constructor in between.
 */
class ModuleServices(
    val events: EventBus,
    val scheduler: Scheduler,
    val commands: CommandRegistry,
    val config: ConfigManager,
    val prices: PriceRepository,
    val hud: HudManager,
    val keybinds: KeyBindManager,
    val neu: NeuRepository,
    val service: ServiceRepository,
)
