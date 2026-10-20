package org.tnozamas.skyatlaz.ui.chat

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.i18n.withGradient
import org.tnozamas.skyatlaz.ui.theme.Gradient
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * The only way the mod writes to the player's chat. A message reads `SkyAtlaz › text`: brand name, a quiet
 * separator and text painted with the gradient of its kind, so the tone is clear at a glance without icons or a
 * wall of color. Messages are client-side only.
 *
 * Client thread only (use the scheduler's `runOnClient` from other threads).
 */
object Chat {
    /** The kinds of message and their body gradients. */
    enum class Kind(
        val gradient: Gradient,
    ) {
        INFO(Theme.INFO_GRADIENT),
        SUCCESS(Theme.SUCCESS_GRADIENT),
        WARNING(Theme.WARNING_GRADIENT),
        ERROR(Theme.ERROR_GRADIENT),
    }

    fun info(message: Component) = send(Kind.INFO, message)

    fun success(message: Component) = send(Kind.SUCCESS, message)

    fun warning(message: Component) = send(Kind.WARNING, message)

    fun error(message: Component) = send(Kind.ERROR, message)

    /** `SkyAtlaz › ` in brand colors; also used by panels that want the mark on a line. */
    fun prefix(): MutableComponent =
        Tr
            .tr("chat.prefix")
            .withGradient(Theme.BRAND_GRADIENT)
            .append(Tr.verbatim(SEPARATOR).withStyle(SUBTLE))

    /** The full message line for [kind]. Built per message (rare), never cached across languages. */
    fun format(
        kind: Kind,
        message: Component,
    ): MutableComponent = prefix().append(message.withGradient(kind.gradient))

    /** A multi-line block sent as one chat message (help pages, summaries). Lines are laid out by the caller. */
    fun block(lines: List<Component>) {
        val message = Tr.verbatim("")
        for ((index, line) in lines.withIndex()) {
            if (index > 0) message.append(NEWLINE)
            message.append(line)
        }
        add(message)
    }

    private fun send(
        kind: Kind,
        message: Component,
    ) = add(format(kind, message))

    @Suppress("UNNECESSARY_SAFE_CALL")
    private fun add(message: Component) {
        // The GUI is not there yet while the mod starts, so a message then would crash the client.
        val chat =
            Minecraft
                .getInstance()
                .gui
                ?.hud
                ?.chat ?: return
        chat.addClientSystemMessage(message)
    }

    private const val SEPARATOR = " › "
    private const val NEWLINE = "\n"
    private val SUBTLE: Style = Style.EMPTY.withColor(Theme.TEXT_SUBTLE)
}
