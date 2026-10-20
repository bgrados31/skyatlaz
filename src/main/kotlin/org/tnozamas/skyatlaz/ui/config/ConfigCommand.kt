package org.tnozamas.skyatlaz.ui.config

import org.tnozamas.skyatlaz.core.command.CommandCategory
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.config.ConfigManager
import org.tnozamas.skyatlaz.core.keybind.KeyBindManager
import org.tnozamas.skyatlaz.core.module.ModuleManager
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.ui.openScreenAfterChat

/** `/skyatlaz config`: opens the real settings screen. */
class ConfigCommand(
    private val config: ConfigManager,
    private val modules: ModuleManager,
    private val keybinds: KeyBindManager,
    private val scheduler: Scheduler,
) {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("config") {
                category(CommandCategory.GENERAL)
                description("command.config")
                executes { openScreenAfterChat(scheduler) { ConfigScreen(config, modules, keybinds) } }
            }
        }
    }
}
