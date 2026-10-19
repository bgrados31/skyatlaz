package org.tnozamas.skyatlaz.core.event

import net.minecraft.network.chat.Component
import org.tnozamas.skyatlaz.util.LegacyText

/**
 * A message from the server (Hypixel sends almost everything as system messages) or the action bar when
 * [actionBar] is true. Cancel it to hide it. [text] and [legacy] are computed on first use and shared by all
 * listeners, so match with a cheap `startsWith`/`contains` on [text] before any regex.
 */
class ChatReceivedEvent(
    val message: Component,
    val actionBar: Boolean,
) : CancellableEvent() {
    /** Plain text without any formatting. */
    val text: String by lazy(LazyThreadSafetyMode.NONE) { LegacyText.strip(message.string) }

    /** Text with legacy `§` codes, for patterns that depend on colors. */
    val legacy: String by lazy(LazyThreadSafetyMode.NONE) { LegacyText.encode(message) }
}
