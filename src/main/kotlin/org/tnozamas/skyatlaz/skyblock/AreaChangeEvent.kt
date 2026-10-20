package org.tnozamas.skyatlaz.skyblock

import org.tnozamas.skyatlaz.core.event.Event

/**
 * The player's area changed (`Hub`, `Dwarven Mines`, ...); [current] is null after leaving SkyBlock or when no
 * scoreboard/tab data is visible yet. Posted before the matching [SkyBlockStateChangeEvent], on the client thread.
 */
class AreaChangeEvent(
    val previous: String?,
    val current: String?,
) : Event
