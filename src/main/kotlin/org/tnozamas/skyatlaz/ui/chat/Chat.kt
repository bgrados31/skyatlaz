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
 * The only way the mod writes to the player's chat. A message reads `SkyAtlaz › ✔ text`: brand name, a quiet
 * separator, an icon in the message's tone and text fading from that tone into regular text, so the kind is
 * clear at a glance without a wall of color. Messages are client-side only.
 *
 * Client thread only (use the scheduler's `runOnClient` from other threads).
 */
object Chat {
    /** The kinds of message, each with its icon and body gradient. Icons are symbols, not language. */
    enum class Kind(
        val icon: String,
        val color: Int,
        val gradient: Gradient,
    ) {
        INFO("✦", Theme.INFO, Theme.INFO_GRADIENT),
        SUCCESS("✔", Theme.SUCCESS, Theme.SUCCESS_GRADIENT),
        WARNING("⚠", Theme.WARNING, Theme.WARNING_GRADIENT),
        ERROR("✖", Theme.ERROR, Theme.ERROR_GRADIENT),
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
    ): MutableComponent =
        prefix()
            .append(Tr.verbatim(kind.icon + " ").withColor(kind.color))
            .append(message.withGradient(kind.gradient))

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

    private fun add(message: Component) {
        Minecraft
            .getInstance()
            .gui.hud.chat
            .addClientSystemMessage(message)
    }

    private const val SEPARATOR = " › "
    private const val NEWLINE = "\n"
    private val SUBTLE: Style = Style.EMPTY.withColor(Theme.TEXT_SUBTLE)
}
