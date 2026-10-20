package org.tnozamas.skyatlaz.skyblock

import org.tnozamas.skyatlaz.core.event.EventBus

/**
 * The single owner of parsed SkyBlock state. Fed the same way by the live game ([updateFromGame]) and by a saved
 * dump ([loadDump]); publishing change events only when the parsed state actually differs, so listeners are cheap
 * and features react instead of polling.
 *
 * Parsing itself lives in [SkyBlockParser]; this class is pure state, so it is unit tested without the game.
 * Client thread only.
 */
class SkyBlockTracker(
    private val bus: EventBus,
) {
    /** Last parsed state; [SkyBlockState.EMPTY] until the first parse or after [reset]. */
    var state: SkyBlockState = SkyBlockState.EMPTY
        private set

    private var lastSources: SkyBlockSources? = null
    private var dumpActive = false

    /** Live game data. Ignored while a dump is loaded, so `/skyatlaz debug load` stays stable offline. */
    fun updateFromGame(sources: SkyBlockSources) {
        if (dumpActive) return
        apply(sources)
    }

    /** Pushes a saved dump into state (offline sandbox); live updates pause until [reset]. */
    fun loadDump(sources: SkyBlockSources) {
        dumpActive = true
        lastSources = sources
        publish(SkyBlockParser.parse(sources))
    }

    /** Forgets everything and leaves dump mode (world left / disconnected). */
    fun reset() {
        dumpActive = false
        lastSources = null
        publish(SkyBlockState.EMPTY)
    }

    private fun apply(sources: SkyBlockSources) {
        if (sources == lastSources) return
        lastSources = sources
        publish(SkyBlockParser.parse(sources))
    }

    private fun publish(next: SkyBlockState) {
        val previous = state
        if (next == previous) return
        state = next
        if (previous.area != next.area) bus.post(AreaChangeEvent(previous.area, next.area))
        bus.post(SkyBlockStateChangeEvent(previous, next))
    }
}
