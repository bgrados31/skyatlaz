package org.tnozamas.skyatlaz.core.debug

import org.tnozamas.skyatlaz.core.event.ChatReceivedEvent
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.Registration

/**
 * Keeps the last [capacity] chat lines (legacy encoded) for dumps. Listens only while debug mode is on, so in
 * normal play it registers nothing and costs nothing.
 */
class ChatRecorder(
    private val bus: EventBus,
    private val capacity: Int = 100,
) {
    private val lines = ArrayDeque<String>(capacity)
    private var registration: Registration? = null

    fun setActive(active: Boolean) {
        if (active && registration == null) {
            registration =
                bus.register<ChatReceivedEvent> { event ->
                    if (event.actionBar) return@register
                    if (lines.size == capacity) lines.removeFirst()
                    lines.addLast(event.legacy)
                }
        } else if (!active) {
            registration?.unregister()
            registration = null
            lines.clear()
        }
    }

    /** Oldest first. */
    fun recent(): List<String> = lines.toList()
}
