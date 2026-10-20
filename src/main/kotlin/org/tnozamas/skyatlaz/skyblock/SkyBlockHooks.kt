package org.tnozamas.skyatlaz.skyblock

import net.minecraft.client.Minecraft
import org.tnozamas.skyatlaz.core.event.EventBus
import org.tnozamas.skyatlaz.core.event.WorldLeaveEvent
import org.tnozamas.skyatlaz.core.scheduler.Scheduler

/**
 * Wires [SkyBlockTracker] to the game: captures the scoreboard/tab every few ticks (state changes are not
 * frame-critical) and clears everything on disconnect. Not a module: this is shared state every feature reads, so
 * it is installed once by the entrypoint.
 */
object SkyBlockHooks {
    /** 10 ticks (0.5 s): Hypixel does not change area/purse faster, and parsing allocates nothing per frame. */
    private const val UPDATE_INTERVAL_TICKS = 10

    /** One second after startup, once the world and tab list are ready. */
    private const val INITIAL_DELAY_TICKS = 20

    fun install(
        tracker: SkyBlockTracker,
        scheduler: Scheduler,
        bus: EventBus,
    ) {
        scheduler.every(UPDATE_INTERVAL_TICKS, initialDelayTicks = INITIAL_DELAY_TICKS) {
            if (Minecraft.getInstance().level == null) return@every
            tracker.updateFromGame(SkyBlockReader.capture())
        }
        bus.register<WorldLeaveEvent> { tracker.reset() }
    }
}
