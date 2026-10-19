package org.tnozamas.skyatlaz.core.debug

import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.client.Minecraft
import org.tnozamas.skyatlaz.core.command.CommandCategory
import org.tnozamas.skyatlaz.core.command.CommandRegistry
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat
import org.tnozamas.skyatlaz.util.LegacyText

/**
 * `/skyatlaz debug ...` tools for developing without Hypixel (docs/TESTING.md):
 * - `dump <name>`: capture scoreboard, tab, recent chat, held item and open container to a fixture file;
 * - `chat <text>`: inject a server message (`&` color codes) through the normal receive pipeline;
 * - `open <name>`: reopen a captured container offline;
 * - `theme`: preview every chat message style.
 * In the DEBUG category, so hidden unless debug mode is on.
 */
class DebugCommands(
    private val scheduler: Scheduler,
    private val recorder: ChatRecorder,
    private val store: SnapshotStore,
) {
    fun register(registry: CommandRegistry) {
        registry.register {
            literal("debug") {
                category(CommandCategory.DEBUG)
                description("command.debug")
                literal("dump") {
                    description("command.debug.dump")
                    argument("name", StringArgumentType.word()) {
                        description("command.debug.dump.name")
                        executes { dump(it.string("name").lowercase()) }
                    }
                }
                literal("chat") {
                    description("command.debug.chat")
                    argument("message", StringArgumentType.greedyString()) {
                        description("command.debug.chat.message")
                        executes { injectChat(it.string("message")) }
                    }
                }
                literal("open") {
                    description("command.debug.open")
                    argument("name", StringArgumentType.word()) {
                        description("command.debug.open.name")
                        suggests { store.names() }
                        executes { open(it.string("name").lowercase()) }
                    }
                }
                literal("theme") {
                    description("command.debug.theme")
                    executes { previewTheme() }
                }
            }
        }
    }

    private fun dump(name: String) {
        if (!SnapshotStore.NAME_PATTERN.matches(name)) {
            Chat.error(Tr.tr("debug.dump.invalid_name", Tr.verbatim(name)))
            return
        }
        // Capture on the client thread (game state), write in the background.
        val snapshot = SnapshotCapture.capture(name, recorder.recent())
        scheduler.async(
            work = { store.save(snapshot) },
            onError = { Chat.error(Tr.tr("debug.dump.failed", Tr.verbatim(name))) },
        ) { file -> Chat.success(Tr.tr("debug.dump.saved", Tr.verbatim(file.fileName.toString()))) }
    }

    private fun injectChat(message: String) {
        val component = Tr.verbatim(LegacyText.fromAmpersand(message))
        Minecraft
            .getInstance()
            .gui
            .chatListener()
            .handleSystemMessage(component, false)
    }

    private fun open(name: String) {
        if (!SnapshotStore.NAME_PATTERN.matches(name)) {
            Chat.error(Tr.tr("debug.dump.invalid_name", Tr.verbatim(name)))
            return
        }
        scheduler.async(
            work = { store.load(name) },
            onError = { Chat.error(Tr.tr("debug.open.failed", Tr.verbatim(name))) },
        ) { snapshot ->
            val container = snapshot?.container
            when {
                snapshot == null -> Chat.error(Tr.tr("debug.dump.missing", Tr.verbatim(name)))

                container == null -> Chat.warning(Tr.tr("debug.open.no_container", Tr.verbatim(name)))

                // Next tick: the chat screen that ran the command closes first and must not close ours.
                else -> scheduler.later(1) { SnapshotContainerScreen.open(container) }
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
