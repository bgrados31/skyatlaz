package org.tnozamas.skyatlaz.core.debug

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.tnozamas.skyatlaz.core.scheduler.Scheduler
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.ui.chat.Chat

/**
 * Saves the state of the game as a dump, with a name taken from what is on screen: the title of the open container
 * (the Heart of the Mountain tree, for one) or, with no container open, the area the player is in.
 *
 * Some states only exist while a container is open, and the chat is blocked while it is, so the player cannot type
 * the dump command. That is why this is also bound to a key that fires with a screen open.
 */
object DebugCapture {
    /**
     * Captures the state and writes it as a dump. The name it used is told in the chat, so the player can load it
     * again with `debug load <name>`.
     */
    fun save(
        scheduler: Scheduler,
        recorder: ChatRecorder,
        store: SnapshotStore,
        area: String?,
    ) {
        val name = name(area)
        val snapshot = SnapshotCapture.capture(name, recorder.recent())
        scheduler.async(
            work = { store.save(snapshot) },
            onError = { Chat.error(Tr.tr("debug.dump.failed", Tr.verbatim(name))) },
        ) { file -> Chat.success(Tr.tr("debug.dump.saved", Tr.verbatim(file.fileName.toString()))) }
    }

    /** The name of the next dump: the title of the open container, or the area, or "capture". */
    fun name(area: String?): String {
        val title = (Minecraft.getInstance().gui.screen() as? AbstractContainerScreen<*>)?.title?.string
        return sanitize(title?.takeIf { it.isNotBlank() } ?: area ?: "")
    }

    /** A file name the store accepts: lowercase letters, digits and underscores. */
    fun sanitize(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text.lowercase()) {
            builder.append(if (char in 'a'..'z' || char in '0'..'9') char else '_')
        }
        return builder
            .toString()
            .trim('_')
            .take(SnapshotStore.NAME_PATTERN_MAX)
            .ifEmpty { "capture" }
    }
}
