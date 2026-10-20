package org.tnozamas.skyatlaz.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

/**
 * Opens [screen] on the next client tick.
 *
 * A client command runs inside `ChatScreen.handleChatInput`; the chat closes immediately after the command returns
 * and would close any screen opened synchronously. Deferring one tick lets the chat finish first.
 */
fun openScreenAfterChat(
    scheduler: Scheduler,
    screen: () -> Screen,
) {
    scheduler.later(1) { Minecraft.getInstance().setScreenAndShow(screen()) }
}
