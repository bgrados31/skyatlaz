package org.tnozamas.skyatlaz.core.debug

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.loader.api.FabricLoader
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

/**
 * `/skyatlaz debug ...` tools for developing without Hypixel (docs/TESTING.md). Registered only in the dev
 * environment for now; a debug-mode config toggle comes with core.config.
 */
object DebugCommands {
    fun register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("skyatlaz").then(
                    ClientCommands.literal("debug").then(
                        ClientCommands.literal("theme").executes {
                            previewTheme()
                            1
                        },
                    ),
                ),
            )
        }
    }

    /** Prints every message kind so theme changes can be checked in game. */
    private fun previewTheme() {
        Chat.info(Tr.tr("debug.theme.info"))
        Chat.success(Tr.tr("debug.theme.success"))
        Chat.warning(Tr.tr("debug.theme.warning"))
        Chat.error(Tr.tr("debug.theme.error"))
    }
}
