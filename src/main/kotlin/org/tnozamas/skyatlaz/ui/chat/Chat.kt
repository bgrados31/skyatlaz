package org.tnozamas.skyatlaz.ui.chat

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.tnozamas.skyatlaz.i18n.Tr
import org.tnozamas.skyatlaz.i18n.withGradient
import org.tnozamas.skyatlaz.ui.theme.Gradient
import org.tnozamas.skyatlaz.ui.theme.Theme

/**
 * The only way the mod writes to the player's chat. Every message gets the brand-gradient prefix and a gradient
 * for its kind, so players recognize SkyAtlaz output and its tone at a glance. Messages are client-side only.
 *
 * Client thread only (use the scheduler's `runOnClient` from other threads).
 */
object Chat {
    fun info(message: Component) = send(Theme.INFO_GRADIENT, message)

    fun success(message: Component) = send(Theme.SUCCESS_GRADIENT, message)

    fun warning(message: Component) = send(Theme.WARNING_GRADIENT, message)

    fun error(message: Component) = send(Theme.ERROR_GRADIENT, message)

    /** Prefix + [message] painted with [gradient]. Built per message (rare), never cached across languages. */
    fun format(
        gradient: Gradient,
        message: Component,
    ): MutableComponent =
        Tr
            .tr("chat.prefix")
            .withGradient(Theme.BRAND_GRADIENT)
            .append(message.withGradient(gradient))

    /** A titled block (help pages, summaries): prefix + brand-gradient [title], then one [lines] entry per line. */
    fun panel(
        title: Component,
        lines: List<Component>,
    ) {
        val message = format(Theme.BRAND_GRADIENT, title)
        for (line in lines) message.append(NEWLINE).append(line)
        add(message)
    }

    private fun send(
        gradient: Gradient,
        message: Component,
    ) = add(format(gradient, message))

    private fun add(message: Component) {
        Minecraft
            .getInstance()
            .gui.hud.chat
            .addClientSystemMessage(message)
    }

    private const val NEWLINE = "\n"
}
