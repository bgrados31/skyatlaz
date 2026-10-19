package org.tnozamas.skyatlaz.ui.chat

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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
    ): Component =
        Tr
            .tr("chat.prefix")
            .withGradient(Theme.BRAND_GRADIENT)
            .append(message.withGradient(gradient))

    private fun send(
        gradient: Gradient,
        message: Component,
    ) {
        Minecraft
            .getInstance()
            .gui.hud.chat
            .addClientSystemMessage(format(gradient, message))
    }
}
