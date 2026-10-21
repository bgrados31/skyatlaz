package org.tnozamas.skyatlaz.core.command

import org.tnozamas.skyatlaz.Skyatlaz
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

/** Commands that exist regardless of modules. `/skyatlaz` itself (the root) prints the help. */
object BuiltinCommands {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("help") {
                category(CommandCategory.GENERAL)
                description("command.help")
                executes { CommandHelp.show(registry, registry.root) }
            }
            literal("version") {
                category(CommandCategory.GENERAL)
                description("command.version")
                executes { Chat.info(Tr.tr("command.version.result", Tr.verbatim(Skyatlaz.VERSION))) }
            }
        }
    }
}
