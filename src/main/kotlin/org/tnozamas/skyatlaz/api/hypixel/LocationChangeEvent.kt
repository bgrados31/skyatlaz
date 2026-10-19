package org.tnozamas.skyatlaz.api.hypixel

import org.tnozamas.skyatlaz.core.event.Event

/**
 * The Hypixel location changed (server switch, joining or leaving Hypixel). [current] is null after leaving.
 * Posted once per change, on the client thread.
 */
class LocationChangeEvent(
    val previous: HypixelLocation?,
    val current: HypixelLocation?,
) : Event
