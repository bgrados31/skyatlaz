package org.tnozamas.skyatlaz.core.debug

import org.tnozamas.skyatlaz.core.command.CommandCategory
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

/**
 * `/skyatlaz debug ...` tools for developing without Hypixel (docs/TESTING.md). In the DEBUG category, so they are
 * hidden unless debug mode is on (always on in the dev environment).
 */
object DebugCommands {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                category(CommandCategory.DEBUG)
                description("command.debug")
                literal("theme") {
                    description("command.debug.theme")
                    executes { previewTheme() }
                }
            }
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
