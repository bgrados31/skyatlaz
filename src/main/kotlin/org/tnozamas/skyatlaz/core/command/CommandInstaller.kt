package org.tnozamas.skyatlaz.core.command

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands

/**
 * Connects the [CommandRegistry] to Fabric's client command dispatcher. Fabric rebuilds the dispatcher on every
 * world join and asks us to register; commands added later (a module enabling mid-session) are merged into the
 * live dispatcher and tab completion is refreshed.
 */
object CommandInstaller {
    fun install(registry: CommandRegistry) {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> registry.install(dispatcher) }
        registry.onTreeChanged = {
            ClientCommands.getActiveDispatcher()?.let { dispatcher ->
                registry.install(dispatcher)
                ClientCommands.refreshCommandCompletions()
            }
        }
    }
}
