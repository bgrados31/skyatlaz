package org.tnozamas.skyatlaz.ui.hud

import org.tnozamas.skyatlaz.core.command.CommandCategory
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.ui.openScreenAfterChat

/** `/skyatlaz hud`: opens the layout editor for the elements currently registered. */
class HudCommand(
    private val hud: HudManager,
    private val scheduler: Scheduler,
) {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("hud") {
                category(CommandCategory.GENERAL)
                description("command.hud")
                executes { openScreenAfterChat(scheduler) { HudEditorScreen(hud) } }
            }
        }
    }
}
